#include "qtquick2controlsapplicationviewer.h"

#include <QColor>
#include <QFile>
#include <QMutex>
#include <QMutexLocker>
#include <QTextStream>
#include <QVector>
#include <QWaitCondition>

#include "disphelper.h"
#include <iostream>
#include <string>
using namespace std;
void testExcel() {
    CDhInitialize init; // Required for the constructor and destructor
    CDispPtr xlApp;
    int i, j;
    VARIANT arr;

    init; // get rid of warning about unused variable
    dhToggleExceptions(TRUE);

    try
    {
        dhCheck( dhCreateObject(L"Excel.Application", NULL, &xlApp) );

        dhPutValue(xlApp, L".Visible = %b", TRUE);

        dhCheck( dhCallMethod(xlApp, L".Workbooks.Add") );

        MessageBoxA(NULL, "First the slow method...", NULL, MB_SETFOREGROUND);

        WITH(xlSheet, xlApp, L"ActiveSheet")
        {
            /* Fill cells with values one by one */
            for (i = 1; i <= 15; i++)
            {
                for (j = 1; j <= 15; j++)
                {
                    dhCheck( dhPutValue(xlSheet, L".Cells(%d,%d) = %d", i, j, i * j) );
                }
            }

        } END_WITH_THROW(xlSheet);

        MessageBoxA(NULL, "Now the fast way...", NULL, MB_SETFOREGROUND);

        /* xlApp.ActiveSheet.Range("A1:O15").Clear */
        dhCallMethod(xlApp, L".ActiveSheet.Range(%S).Clear", L"A1:O15");

        /* Create a safe array of VARIANT[15][15] */
        {
           SAFEARRAYBOUND sab[2];

           arr.vt = VT_ARRAY | VT_VARIANT;              /* An array of VARIANTs. */
           sab[0].lLbound = 1; sab[0].cElements = 15;   /* First dimension.  [1 to 15] */
           sab[1].lLbound = 1; sab[1].cElements = 15;   /* Second dimension. [1 to 15] */
           arr.parray = SafeArrayCreate(VT_VARIANT, 2, sab);
        }

        /* Now fill in the array */
        for(i=1; i <= 15; i++)
        {
            for(j=1; j <= 15; j++)
            {
                VARIANT tmp = {};
                long indices[2];

                indices[0] = i;  /* Index of first dimension */
                indices[1] = j;  /* Index of second dimension */

                tmp.vt = VT_I4;
                tmp.lVal = i * j + 10;

                SafeArrayPutElement(arr.parray, indices, (void*)&tmp);
            }
        }

        /* Set all values in one shot! */
        /* xlApp.ActiveSheet.Range("A1:O15") = arr */
        dhCheck( dhPutValue(xlApp, L".ActiveSheet.Range(%S) = %v", L"A1:O15", &arr) );

        VariantClear(&arr);
    }
    catch (string errstr)
    {
        cerr << "Fatal error details:" << endl << errstr << endl;
    }

    dhToggleExceptions(FALSE);

    dhPutValue(xlApp, L".ActiveWorkbook.Saved = %b", TRUE);
}
const int STATUS_LOADING = 2;
const int STATUS_BUSY = 0x00020000;
const int STATUS_RUNNING = 0x0100;
const int STATUS_EXECUTIONERROR = 0x2000;
const int STATUS_STOPPED = 0x00080000;

class Evoware {
    CDhInitialize init; // Required for the constructor and destructor
    CDispPtr evodb;
    CDispPtr evosys;

public:
    Evoware() {
        dhToggleExceptions(TRUE);
    }

    ~Evoware() {
        dhToggleExceptions(FALSE);
    }

    /// Establish connection to Evoware COM object
    void connect() {
        try {
            dhCheck(dhCreateObject(L"evoapi.database", NULL, &evodb));
            dhCheck(dhCreateObject(L"evoapi.system", NULL, &evosys));
        }
        catch (string errstr) {
            cerr << "Fatal error details:" << endl << errstr << endl;
        }
    }

    /// Logon to evoware
    void logon() {
        try {
            dhCheck(dhCallMethod(evosys, L".Logon(%S, %S, %d, %d)", L"apiuser", L"123456", 0, 0));
        }
        catch (string errstr) {
            cerr << "Fatal error details:" << endl << errstr << endl;
        }
    }

    /// Logoff of evoware
    void logoff() {
        try {
            dhCheck(dhCallMethod(evosys, L".Logoff()"));
        }
        catch (string errstr) {
            cerr << "Fatal error details:" << endl << errstr << endl;
        }
    }

    /// Tell evoware to initialize the robot
    void initialize() {
        try {
            //dhCheck(dhCallMethod(evosys, L".Initialize()"));
            // NOTE: Removed dhCheck because Initialize always returns an error...
            dhCallMethod(evosys, L".Initialize()");
        }
        catch (string errstr) {
            cerr << "Fatal error details:" << endl << errstr << endl;
        }
    }

    bool isReady() {
        long status;
        try {
            dhCheck(dhGetValue(L"%d", &status, evosys, L".GetStatus"));
            return !(
                (status & STATUS_LOADING) ||
                ((status >> 16) & (STATUS_BUSY >> 16))
            );
        }
        catch (string errstr) {
            cerr << "Fatal error details:" << endl << errstr << endl;
        }
        return false;
    }

    void sleep(int msec) {
        QWaitCondition wc;
        QMutex mutex;
        QMutexLocker locker(&mutex);
        wc.wait(&mutex, msec);
    }

    void waitTillReady() {
        while (!isReady())
            sleep(300);
    }

    int prepareScript(const QString& filename) {
        // TODO: remove recovery files to ignore previous failures
        // os.system('del /F "C:\Program Files\TECAN\EVOware\database\scripts\*.rec"')
        long scriptId = -1;
        try {
            QByteArray filenameData = filename.toLatin1();
            const char* szFilename = filenameData.constData();
            dhCheck(dhGetValue(L"%d", &scriptId, evosys, L".PrepareScript(%s)", szFilename));
        }
        catch (string errstr) {
            cerr << "Fatal error details:" << endl << errstr << endl;
        }
        return scriptId;
    }

    bool startScript(const QString& filename) {
        bool result = true;
        int scriptId = prepareScript(filename);
        if (scriptId > -1) {
            try {
                dhCheck(dhCallMethod(evosys, L".StartScript(%d,%d,%d)", scriptId, 0, 0));
            }
            catch (string errstr) {
                cerr << "Fatal error details:" << endl << errstr << endl;
            }
        }
    }

    void tryit() {
        try {
            dhCheck(dhCreateObject(L"evoapilib.script", NULL, &evodb));
            dhCheck(dhCreateObject(L"evoapi.system", NULL, &evosys));
        }
        catch (string errstr) {
            cerr << "Fatal error details:" << endl << errstr << endl;
        }
    }
};

void saveWorklist(const QVector<QColor>& color_l) {
    QFile file("C:\\Ellis\\openhouse.gwl");
    file.open(QIODevice::WriteOnly | QIODevice::Text);

    QTextStream out(&file);
    int aspirate[4];
    QString dispense[4];

    for (int i = 0; i < 4; i++)
        aspirate[i] = 0;

    for (int i = 0; i < color_l.size(); i++) {
        const int tip_i = i % 4;
        const int tip_n = tip_i + 1;
        const int well_n = i + 1;
        const int volume = (255 - qGray(color_l[i].rgb())) * 100 / 255;
        if (aspirate[tip_i] + volume > 900) {
            for (int tip_i = 0; tip_i < 4; tip_i++) {
                out << "A;T2;;;8;;" << aspirate[tip_i] << ";Water free dispense;;" << tip_n << endl;
                out << dispense[tip_i];
                out << "W1;" << endl;
                aspirate[tip_i] = 0;
                dispense[tip_i].clear();
            }
        }
        aspirate[tip_i] += volume;
        if (volume > 0) {
            QTextStream out2(&dispense[tip_i]);
            out2 << "D;plate;;;" << well_n << ";;" << volume << ";Water free dispense;;" << tip_n << endl;
        }
    }
    for (int tip_i = 0; tip_i < 4; tip_i++) {
        const int tip_n = tip_i + 1;
        out << "A;T2;;;8;;" << aspirate[tip_i] << ";Water free dispense;;" << tip_n << endl;
        out << dispense[tip_i];
        out << "W1;" << endl;
        aspirate[tip_i] = 0;
        dispense[tip_i].clear();
    }
}

class Interface2 : public QObject {
    Q_OBJECT

private:
    QVector<QColor> color_l;

public:
    Interface2()
        : color_l(96)
    {
        for (int i = 0; i < 96; i++)
            color_l[i] = QColor(255, 255, 255);
    }

    Q_INVOKABLE void setColor(const int row, const int col, const int r, const int g, const int b) {
        const int i = col * 8 + row;
        color_l[i] = QColor(r, g, b);
    }
};


int main(int argc, char *argv[])
{
    Application app(argc, argv);


    QtQuick2ControlsApplicationViewer viewer;
    //viewer.setInterface(interface);
    viewer.setMainQmlFile(QStringLiteral("qml/Relay1/main.qml"));
    viewer.show();

    /*
    {
        CDhInitialize init; // Required for the constructor and destructor
        CDispPtr evoscript;
        try {
            dhCheck(dhCreateObject(L"EVOAPILib.Script", NULL, &evoscript));
            cout << "A";
        }
        catch (string errstr) {
            cerr << "Fatal error details:" << endl << errstr << endl;
        }
        try {
            dhCheck(dhCreateObject(L"EVOAPILib.ScriptClass", NULL, &evoscript));
            cout << "A";
        }
        catch (string errstr) {
            cerr << "Fatal error details:" << endl << errstr << endl;
        }
    }
    */

    QVector<QColor> color_l(96);
    for (int i = 0; i < 96; i++)
        color_l[i] = QColor(255, 255, 255);
    color_l[4] = QColor(0, 0, 0);
    color_l[5] = QColor(0, 0, 0);
    color_l[6] = QColor(0, 0, 0);
    color_l[11] = QColor(0, 0, 0);
    color_l[12] = QColor(0, 0, 0);
    color_l[16] = QColor(0, 0, 0);
    color_l[18] = QColor(0, 0, 0);
    color_l[19] = QColor(0, 0, 0);
    color_l[20] = QColor(0, 0, 0);
    color_l[21] = QColor(0, 0, 0);
    color_l[22] = QColor(0, 0, 0);
    saveWorklist(color_l);

    /*
    Evoware evoware;
    evoware.connect();
    evoware.logon();
    evoware.waitTillReady();
    evoware.initialize();
    evoware.startScript("C:\\Program Files\\TECAN\\EVOware\\database\\scripts\\Ellis\\worklisttest.esc");
    //evoware.prepareScript("C:\\Program Files\\TECAN\\EVOware\\database\\scripts\\Roboliq\\Roboliq_Clean_Light_1000.esc");
    //evoware.startScript("C:\\Program Files\\TECAN\\EVOware\\database\\scripts\\Roboliq\\Roboliq_Clean_Light_1000.esc");
    //evoware.prepareScript("W:\\roboliq\\tania01_ph_r1.esc");
    evoware.logoff();
    */

    return app.exec();
    return 0;
}
