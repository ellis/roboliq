#include "backend.h"

#include <QFile>
#include <QImage>
#include <QTextStream>
#include <QUrl>
#include <QtDebug>


Backend::Backend(QObject *parent) :
    QObject(parent)
{
    //setSize(8, 12);
    setPlate(3);
}

QString Backend::getFillStyle(const int row, const int col) {
    const int i = col * m_rowCount + row;
    return color_l[i].name();
}

void Backend::setSize(int rowCount, int colCount) {
    //if (m_rowCount != rowCount || m_colCount != colCount)
    m_rowCount = rowCount;
    m_colCount = colCount;
    color_l.resize(m_rowCount * m_colCount);
    for (int i = 0; i < m_rowCount * m_colCount; i++)
          color_l[i] = QColor(255, 255, 255);
    emit rowCountChanged(m_rowCount);
    emit colCountChanged(m_colCount);
}

void Backend::setPlate(int plate_i) {
    switch (plate_i) {
    // 386 well very low volume
    case 2:
        m_volumeSaturated = 3;
        m_volumeTotal = 20;
        setSize384();
        break;
    // 386 well standard, use low volume
    case 3:
        m_volumeSaturated = 5;
        m_volumeTotal = 10;
        setSize384();
        break;
    // 386 well standard, use high volume
    case 4:
        m_volumeSaturated = 5;
        m_volumeTotal = 80;
        setSize384();
        break;
    // 96 well, 100ul
    default:
        m_volumeTotal = 100;
        setSize96();
        break;
    }
}

void Backend::setSize96() {
    setSize(8, 12);
}

void Backend::setSize384() {
    setSize(16, 24);
}

void Backend::setColor(const int row, const int col, const QString& colorName) {
    const QColor color(colorName);
    const int i = col * m_rowCount + row;
    color_l[i] = color;
}

void Backend::openImage(const QString& url) {
    const QUrl qurl(url);
    const QString filename = qurl.toLocalFile();
    QImage image(filename);

    setSize(image.size().height(), image.size().width());

    int i = 0;
    for (int col = 0; col < m_colCount; col++) {
        for (int row = 0; row < m_rowCount; row++) {
            const QRgb rgb = image.pixel(col, row);
            color_l[i++] = QColor(rgb);
        }
    }
}

void Backend::saveImage(const QString& filename) {
    QImage image(m_colCount, m_rowCount, QImage::Format_RGB32);

    int i = 0;
    for (int col = 0; col < m_colCount; col++) {
        for (int row = 0; row < m_rowCount; row++) {
            image.setPixel(col, row, color_l[i++].rgb());
        }
    }

    image.save(filename);
}

void Backend::saveWorklistSepia() {
    //QFile file("C:\\Ellis\\openhouse.gwl");
    QFile file("openhouse.gwl");
    file.open(QIODevice::WriteOnly | QIODevice::Text);

    QTextStream out(&file);
    int aspirate[4];
    QString dispense[4];

    for (int i = 0; i < 4; i++)
        aspirate[i] = 0;

    for (int i = 0; i < color_l.size(); i++) {
        const int tip_i = i % 4;
        //const int tip_n = tip_i + 1;
        const int tip_mask = 1 << tip_i;
        const int well_n = i + 1;
        const int volume = (255 - qGray(color_l[i].rgb())) * m_volumeTotal / 255;
        if (aspirate[tip_i] + volume > 900) {
            for (int tip_i = 0; tip_i < 4; tip_i++) {
                const int tip_mask = 1 << tip_i;
                out << "A;T2;;;8;;" << aspirate[tip_i] << ";Water free dispense;;" << tip_mask << endl;
                out << dispense[tip_i];
                out << "W1;" << endl;
                aspirate[tip_i] = 0;
                dispense[tip_i].clear();
            }
        }
        aspirate[tip_i] += volume;
        if (volume > 0) {
            QTextStream out2(&dispense[tip_i]);
            out2 << "D;plate;;;" << well_n << ";;" << volume << ";Water free dispense;;" << tip_mask << endl;
        }
    }
    for (int tip_i = 0; tip_i < 4; tip_i++) {
        //const int tip_n = tip_i + 1;
        const int tip_mask = 1 << tip_i;
        out << "A;T2;;;8;;" << aspirate[tip_i] << ";Water free dispense;;" << tip_mask << endl;
        out << dispense[tip_i];
        out << "W1;" << endl;
        aspirate[tip_i] = 0;
        dispense[tip_i].clear();
    }
}

void Backend::printWorklistItems(
    QTextStream& out,
    const int n,
    double aspirate[],
    const int tipWell[],
    const QString tipSite[],
    const QString tipLiquidClass[8],
    QString dispense[])
{
    for (int tip_i = 0; tip_i < n; tip_i++) {
        if (aspirate[tip_i] > 0) {
            const int tip_mask = 1 << tip_i;
            out << "A;" << tipSite[tip_i] << ";;;" << tipWell[tip_i] << ";;" << aspirate[tip_i] << ";"
                   << tipLiquidClass[tip_i] << ";;" << tip_mask << endl;
            out << dispense[tip_i];
            out << "W1;" << endl;
        }
        aspirate[tip_i] = 0;
        dispense[tip_i].clear();
    }
}

const int tipWell[8] = { 1, 2, 3, 4, 5, 6, 7, 8 };
const QString tipSite[8] = { "water", "water", "water", "water", "T3", "T3", "T3", "T3" };
const QString tipLiquidClass[8] = { "Water free dispense", "Water free dispense", "Water free dispense", "Water free dispense",
                                    "Water_C_50", "Water_C_50", "Water_C_50", "Water_C_50" };

void Backend::saveWorklistColor3() {
    //QFile file("C:\\Ellis\\openhouse.gwl");
    QFile file("openhouse.gwl");
    file.open(QIODevice::WriteOnly | QIODevice::Text);

    QTextStream out(&file);
    double aspirate[8];
    QString dispense[8];

    for (int i = 0; i < 8; i++)
        aspirate[i] = 0;

    for (int i = 0; i < color_l.size(); i++) {
        const int well_n = i + 1;

        // Simplify the color, only use one of red, green, OR blue
        const QColor& color0 = color_l[i];
        QColor color = color_l[i];
        const int tipWater_i = i % 4;
        int tipColor_i = 0;
        if (color.red() >= color.blue() && color.red() >= color.green()) {
            color.setRgb(color.red(), 0, 0);
            tipColor_i = 4;
        }
        else if (color.green() >= color.blue()) {
            color.setRgb(0, color.green(), 0);
            tipColor_i = 5;
        }
        else {
            color.setRgb(0, 0, color.blue());
            tipColor_i = 6;
        }

        double volumeColor = ((double) ((int) (m_volumeSaturated * color0.saturationF() * 10))) / 10;
        qDebug() << i << color0 << color << volumeColor;
        if (volumeColor < 0.5)
            volumeColor = 0;
        if (volumeColor > 0) {
            double volumeWater = m_volumeTotal - volumeColor;
            if (volumeWater < 3)
                volumeWater = 0;

            if (aspirate[tipWater_i] + volumeWater > 950 || aspirate[tipColor_i] > 0) {
                printWorklistItems(out, 8, aspirate, tipWell, tipSite, tipLiquidClass, dispense);
            }
            aspirate[tipWater_i] += volumeWater;
            aspirate[tipColor_i] += volumeColor;

            if (volumeWater > 0) {
                QTextStream outWater(&dispense[tipWater_i]);
                outWater << "D;plate;;;" << well_n << ";;" << volumeWater << ";" << tipLiquidClass[tipWater_i] << ";;" << (1 << tipWater_i) << endl;
            }
            QTextStream outColor(&dispense[tipColor_i]);
            outColor << "D;plate;;;" << well_n << ";;" << volumeColor << ";" << tipLiquidClass[tipColor_i] << ";;" << (1 << tipColor_i) << endl;
        }
    }
    printWorklistItems(out, 8, aspirate, tipWell, tipSite, tipLiquidClass, dispense);
}

void Backend::saveWorklistColor3LargeTips() {
    //QFile file("C:\\Ellis\\openhouse.gwl");
    QFile file("openhouse.gwl");
    file.open(QIODevice::WriteOnly | QIODevice::Text);

    QTextStream out(&file);
    int aspirate[4];
    QString dispense[4];

    for (int i = 0; i < 4; i++)
        aspirate[i] = 0;

    for (int i = 0; i < color_l.size(); i++) {
        const int well_n = i + 1;

        // Simplify the color, only use one of red, green, OR blue
        QColor color = color_l[i];
        int tip_i = 0;
        if (color.red() >= color.blue() && color.red() >= color.green()) {
            color.setRgb(color.red(), 0, 0);
            tip_i = 0;
        }
        else if (color.green() >= color.blue()) {
            color.setRgb(0, color.green(), 0);
            tip_i = 1;
        }
        else {
            color.setRgb(0, 0, color.blue());
            tip_i = 2;
        }

        const int volume = (int) (m_volumeTotal * color.saturationF());
        const int tip_mask = 1 << tip_i;
        if (aspirate[tip_i] + volume > 900) {
            for (int tip_i = 0; tip_i < 4; tip_i++) {
                const int tip_mask = 1 << tip_i;
                out << "A;T3;;;" << (tip_i + 5) << ";;" << aspirate[tip_i] << ";Water free dispense;;" << tip_mask << endl;
                out << dispense[tip_i];
                out << "W1;" << endl;
                aspirate[tip_i] = 0;
                dispense[tip_i].clear();
            }
        }
        aspirate[tip_i] += volume;
        if (volume > 0) {
            QTextStream out2(&dispense[tip_i]);
            out2 << "D;plate;;;" << well_n << ";;" << volume << ";Water free dispense;;" << tip_mask << endl;
        }
    }
    for (int tip_i = 0; tip_i < 4; tip_i++) {
        //const int tip_n = tip_i + 1;
        const int tip_mask = 1 << tip_i;
        out << "A;T3;;;" << (tip_i + 5) << ";;" << aspirate[tip_i] << ";Water free dispense;;" << tip_mask << endl;
        out << dispense[tip_i];
        out << "W1;" << endl;
        aspirate[tip_i] = 0;
        dispense[tip_i].clear();
    }
}
