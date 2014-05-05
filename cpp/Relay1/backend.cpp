#include "backend.h"

#include <QFile>
#include <QImage>
#include <QTextStream>
#include <QUrl>


Backend::Backend(QObject *parent) :
    QObject(parent)
  , color_l(96)
{
}

QString Backend::getFillStyle(const int row, const int col) {
    const int i = col * 8 + row;
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

void Backend::setSize96() {

}

void Backend::setSize384() {

}

void Backend::setColor(const int row, const int col, const QString& colorName) {
    const QColor color(colorName);
    const int i = col * 8 + row;
    color_l[i] = color;
}

void Backend::openImage(const QString& url) {
    const QUrl qurl(url);
    const QString filename = qurl.toLocalFile();
    QImage image(filename);

    int i = 0;
    for (int col = 0; col < 12; col++) {
        for (int row = 0; row < 8; row++) {
            const QRgb rgb = image.pixel(col, row);
            color_l[i++] = QColor(rgb);
        }
    }
}

void Backend::saveImage(const QString& filename) {
    QImage image(12, 8, QImage::Format_RGB32);

    int i = 0;
    for (int col = 0; col < 12; col++) {
        for (int row = 0; row < 8; row++) {
            image.setPixel(col, row, color_l[i++].rgb());
        }
    }

    image.save(filename);
}

void Backend::saveWorklist() {
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
