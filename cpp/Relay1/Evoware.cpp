#include <QWaitCondition>
#include <QMutex>
#include <QMutexLocker>

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
            dhCheck(dhCallMethod(evosys, L".Initialize()"));
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
