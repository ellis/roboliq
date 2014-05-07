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
	return QColor(m_image.pixel(col, row)).name();
}

void Backend::setSize(int rowCount, int colCount) {
    //if (m_rowCount != rowCount || m_colCount != colCount)
	m_image = QImage(colCount, rowCount, QImage::Format_RGB32);
	m_image.fill(Qt::white);
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
	m_image.setPixel(col, row, color.rgb());
}

static int interpolator(int a, int b, float t)
{
	return a * (1 - t) + b * t;
}

static QColor interpolate(const QColor& a, const QColor& b, float t)
{
	// 0.0 <= t <= 1.0
	const QColor ca = a.toHsv();
	const QColor cb = b.toHsv();
	const QColor final = QColor::fromHsv(
		interpolator(ca.hue(), cb.hue(), t),
		interpolator(ca.saturation(), cb.saturation(), t),
		interpolator(ca.value(), cb.value(), t)
	);
	return final.toRgb();
}

void Backend::colorizeMonochrome() {
	const QColor sepia(112, 66, 20);
	const QColor white = Qt::white;

	for (int col = 0; col < m_image.width(); col++) {
		for (int row = 0; row < m_image.height(); row++) {
			const QColor color0 = m_image.pixel(col, row);
			const int intensity = qGray(color0.rgb());
			const QColor color1 = interpolate(sepia, white, intensity / 255.0f);
			m_image.setPixel(col, row, color1.rgb());
		}
	}
}

void Backend::colorizeHues3() {

}

void Backend::colorizeHues6() {

}

void Backend::openImage(const QString& url) {
    const QUrl qurl(url);
    const QString filename = qurl.toLocalFile();
	m_image.load(filename);
}

void Backend::saveImage(const QString& filename) {
	m_image.save(filename);
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

	for (int col = 0; col < m_image.width(); col++) {
		for (int row = 0; row < m_image.height(); row++) {
			const int i = col * m_image.height() + row;
			const int tip_i = i % 4;
			//const int tip_n = tip_i + 1;
			const int tip_mask = 1 << tip_i;
			const int well_n = i + 1;
			const QColor color = m_image.pixel(col, row);
			const QRgb rgb = color.rgb();
			const int volume = (255 - qGray(rgb)) * m_volumeTotal / 255;
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

	for (int col = 0; col < m_image.width(); col++) {
		for (int row = 0; row < m_image.height(); row++) {
			const int i = col * m_image.height() + row;
			const int well_n = i + 1;

			// Simplify the color, only use one of red, green, OR blue
			const QColor& color0 = m_image.pixel(col, row);
			QColor color = color0;
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

	for (int col = 0; col < m_image.width(); col++) {
		for (int row = 0; row < m_image.height(); row++) {
			const int i = col * m_image.height() + row;
			const int well_n = i + 1;

			// Simplify the color, only use one of red, green, OR blue
			QColor color = m_image.pixel(col, row);
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
