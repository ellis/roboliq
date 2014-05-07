#include "qtquick2controlsapplicationviewer.h"

#include <QQmlContext>
#include <QQmlEngine>

#include "backend.h"

#include <QtDebug>
#include <QImage>
#include <QUrl>

extern QRgb ryb2rgb(qreal R, qreal Y, qreal B);
extern QRgb rgb2ryb(const QColor& color);


int main(int argc, char *argv[])
{
    Application app(argc, argv);

    /*qDebug() << QColor(Qt::white);
	qDebug() << QColor(rgb2ryb(Qt::white)).name();
	qDebug() << QColor(rgb2ryb(Qt::black)).name();
	qDebug() << QColor(rgb2ryb(Qt::red)).name();
	qDebug() << QColor(rgb2ryb(Qt::green)).name();
	qDebug() << QColor(rgb2ryb(Qt::blue)).name();
    qDebug() << QColor(rgb2ryb(Qt::yellow)).name();*/
    qDebug() << QColor(ryb2rgb(1, 0, 0)).name();
    qDebug() << QColor(ryb2rgb(0, 1, 0)).name();
    qDebug() << QColor(ryb2rgb(0, 0, 1)).name();
    qDebug() << QColor(ryb2rgb(.5, .5, 0)).name();
    qDebug() << QColor(ryb2rgb(.5, 0, .5)).name();
    qDebug() << QColor(ryb2rgb(0, .5, .5)).name();
    //return 0;

    QtQuick2ControlsApplicationViewer viewer;
    Backend* backend = new Backend();
    //viewer.setInterface(interface);
    viewer.getEngine().rootContext()->setContextProperty("backend", backend);
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

    /*QVector<QColor> color_l(96);
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
    saveWorklist(color_l);*/

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
