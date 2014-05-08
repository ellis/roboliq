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

QRgb ryb2rgb(qreal R, qreal Y, qreal B) {
  R = R*R*(3-R-R);
  Y = Y*Y*(3-Y-Y);
  B = B*B*(3-B-B);
  return QColor::fromRgbF(1.0 + B * ( R * (0.337 + Y * -0.137) + (-0.837 + Y * -0.163) ),
	1.0 + B * ( -0.627 + Y * 0.287) + R * (-1.0 + Y * (0.5 + B * -0.693) - B * (-0.627) ),
    1.0 + B * (-0.4 + Y * 0.6) - Y + R * ( -1.0 + B * (0.9 + Y * -1.1) + Y )).rgb();
}

QRgb rgb2ryb0(const QColor& color) {
	qreal r = color.redF(),
		g = color.greenF(),
		b = color.blueF();
	// Remove the whiteness from the color.
	const qreal w = qMin(qMin(r, g), b);
	r -= w;
	g -= w;
	b -= w;

	const qreal mg = qMax(qMax(r, g), b);

	// Get the yellow out of the red+green.
	qreal y = qMin(r, g);
	r -= y;
	g -= y;

	// If this unfortunate conversion combines blue and green, then cut each in
	// half to preserve the value's maximum range.
	if (b > 0 && g > 0) {
		b /= 2;
		g /= 2;
	}

	// Redistribute the remaining green.
	y += g;
	b += g;

	// Normalize to values.
	const qreal my = qMax(qMax(r, y), b);
	if (my > 0) {
		qreal n = mg / my;
		r *= n;
		y *= n;
		b *= n;
	}

	// Add the white back in.
	r += w;
	y += w;
	b += w;

	// And return back the ryb typed accordingly.
	//return qRgb(255 * r, 255 * y, 255 * b);
	return QColor::fromRgbF(1-r, 1-y, 1-b).rgb();
}

int distHue(int hueTarget, int hueActual) {
    int d = ((hueTarget + 360) - hueActual) % 360;
    if (d > 180)
        d = 360 - d;
    return d;
}

QRgb rgb2ryb(const QColor& color) {
    const int hue = color.hslHue();
    const int dRed = distHue(0, hue);
    const int dYellow = distHue(60, hue);
    const int dGreen = distHue(120, hue);
    const int dBlue = distHue(240, hue);
    const int dPurple = distHue(300, hue);

    const int dMin = qMin(dRed, qMin(dYellow, qMin(dGreen, qMin(dBlue, dPurple))));
    if (dRed == dMin) {

    }
    /*const QColor
            C000 = QColor::fromRgbF(1.0, 1.0, 1.0), // White
            C100 = QColor::fromRgbF(1.0, 0.0, 0.0), // Red
            C010 = QColor::fromRgbF(1.0, 1.0, 0.0), // Yellow
            C001 = QColor::fromRgbF(1.0, 1.0, 1.0), // Blue
            C110 = QColor::fromRgbF(1.0, 0.5, 0.0), // Orange
            C101 = QColor::fromRgbF(0.5, 0.5, 1.0), // Purple
            C011 = QColor::fromRgbF(0.0, 0.0, 0.0); // Black*/
    qreal r = color.redF(),
        g = color.greenF(),
        b = color.blueF();
    // Remove the whiteness from the color.
    const qreal w = qMin(qMin(r, g), b);
    r -= w;
    g -= w;
    b -= w;

    const qreal mg = qMax(qMax(r, g), b);

    // Get the yellow out of the red+green.
    qreal y = qMin(r, g);
    r -= y;
    g -= y;

    // If this unfortunate conversion combines blue and green, then cut each in
    // half to preserve the value's maximum range.
    if (b > 0 && g > 0) {
        b /= 2;
        g /= 2;
    }

    // Redistribute the remaining green.
    y += g;
    b += g;

    // Normalize to values.
    const qreal my = qMax(qMax(r, y), b);
    if (my > 0) {
        qreal n = mg / my;
        r *= n;
        y *= n;
        b *= n;
    }

    // Add the white back in.
    r += w;
    y += w;
    b += w;

    // And return back the ryb typed accordingly.
    //return qRgb(255 * r, 255 * y, 255 * b);
    return QColor::fromRgbF(1-r, 1-y, 1-b).rgb();
}


struct RYBK {
    qreal R;
    qreal Y;
    qreal B;
    qreal K;
    QColor colorExpected;

    RYBK(qreal R = 0, qreal Y = 0, qreal B = 0, qreal K = 0, const QColor& colorExpected = Qt::white)
        : R(R), Y(Y), B(B), K(K), colorExpected(colorExpected)
    {
    }
};

QVector<RYBK> colorInfo {
    RYBK(20.0/96, 0, 0, 0, QColor("#d20313")), // red hue=355
    RYBK(0, 10.0/96, 0, 0, QColor("#fffd54")), // yellow
    RYBK(0, 0, 30.0/96, 0, QColor("#0067c3")), // blue
    RYBK(0, 15.0/96, 15.0/96, 0, QColor("#018807")), // green
    RYBK(2.0/96, 30.0/96, 0, 0, QColor("#f07702")), // redish-orange hue=29
};

RYBK convertColor(const QColor& color) {
    const QColor color1 = color.toHsv();
    if (color1.saturation() < 10) {
        // FIXME: handle shades of grey
        return RYBK(0, 0, 0, 0, Qt::white);
    }
    const int hue = color1.hue();
    int dMin = distHue(colorInfo[0].colorExpected.hue(), hue);
    const RYBK* ci = &colorInfo[0];
    for (const RYBK& ci_ : colorInfo) {
        const int d = distHue(ci_.colorExpected.hue(), hue);
        //qDebug() << "distHue(" << ci_.colorExpected.hue() << ", " << hue << ") = " << d;
        if (d < dMin) {
            dMin = d;
            ci = &ci_;
        }
    }
    RYBK rybk = *ci;
    const QColor color2 = ci->colorExpected.toHsv();

    const int hue2 = color2.hue();
    const int saturation2 = qMin(color.hsvSaturation(), color2.hsvSaturation());
    if (color.saturation() < color2.saturation()) {
        qreal factor = color.saturationF() / color2.saturationF();
        rybk.R *= factor;
        rybk.Y *= factor;
        rybk.B *= factor;
        rybk.K *= factor;
    }
    //const int value2 = qMin(color.value(), color2.value());
    const int value2 = color2.value();
    //if (color.value() < color2.value()) {
        // TODO: Add black ink
    //}
    //qDebug() << "rybk" << rybk.R << rybk.Y << rybk.B << rybk.K;

    rybk.colorExpected = QColor::fromHsv(hue2, saturation2, value2);
    return rybk;
}

QColor reduceColor(const QColor& color) {
    const RYBK rybk = convertColor(color);
    return rybk.colorExpected;
}

/*
QColor reduceColor(const QColor& color) {
    const int hue = color.hslHue();
    const int dRed = distHue(0, hue);
    const int dYellow = distHue(60, hue);
    const int dGreen = distHue(120, hue);
    const int dBlue = distHue(240, hue);
    const int dPurple = distHue(300, hue);

    B10 = [-100, -2.6, -0.9]


    const int dMin = qMin(dRed, qMin(dYellow, qMin(dGreen, qMin(dBlue, dPurple))));
    QColor color2;
    if (dRed == dMin)
        color2 = QColor::fromRgb(0xcc, 0, 0); // 30ul R
    else if (dYellow == dMin)
        color2 = QColor::fromRgb(0xeb, 0xd3, 0); // 30ul Y
    else if (dGreen == dMin)
        color2 = QColor::fromRgb(0, 0x9b, 0x0f); // 10ul Y, 10ul B
    else if (dBlue == dMin)
        color2 = QColor::fromRgb(0,0x79, 0xc6); // 30ul B
    else
        color2 = QColor::fromRgb(0x80, 0x45, 0x9a); // 5ul R, 5ul B
    const int hue2 = color2.hslHue();

    const int saturation2 = color2.hslSaturation();
    //const int saturation2 = qMin(color.hslSaturation(), color2.hslSaturation());

    const int lightness2 = (color.lightness() > color2.lightness()) ? color.lightness() : color2.lightness();

    return QColor::fromHsl(hue2, saturation2, lightness2);
}
*/

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
    for (int col = 0; col < m_image.width(); col++) {
		for (int row = 0; row < m_image.height(); row++) {
			const QColor color0 = m_image.pixel(col, row);
            //const QRgb ryb = rgb2ryb(color0);
            //const QRgb rgb = ryb2rgb(qRed(ryb), qGreen(ryb), qBlue(ryb));
            const QRgb rgb = reduceColor(color0).rgb();
			m_image.setPixel(col, row, rgb);
		}
    }
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

void setupSourceVolumeColor(SourceVolume& sv, qreal amount, int row) {
    sv.plate = "T3";
    sv.row = row;

    if (amount * 96 < 1) {
        sv.col = -1;
        sv.volume = 0;
    }
    else {
        sv.col = 2;
        sv.volume = amount * 96;
    }
    if (sv.volume > 30) {
        qDebug() << "Large amount " << sv.volume;
    }
}

void Backend::printWorklistPipetteItems(
    QTextStream& out,
    QVector<Pipette>& pipette_l
) {
    for (int i = 0; i < pipette_l.size(); i++) {
        Pipette& pipette = pipette_l[i];
        const TipWellVolumePolicy& src = pipette.src;
        if (src.volume > 0) {
            const int tip_mask = 1 << src.tip;
            // Number of rows on source plate
            int rowCount = 0;
            if (src.plate == "water") rowCount = 8;
            else if (src.plate == "T3") rowCount = 4;

            const int srcWell_n = 1 + src.row + src.col * rowCount;
            //qDebug() << srcWell_n << src.row << src.col << rowCount;
            out << "A;" << src.plate << ";;;" << srcWell_n << ";;" << src.volume << ";"
                   << src.policy << ";;" << tip_mask << endl;
            for (const TipWellVolumePolicy& dst : pipette.dst_l) {
                const int dstWell_n = 1 + dst.row + dst.col * m_image.height();
                out << "D;" << dst.plate << ";;;" << dstWell_n << ";;" << dst.volume << ";"
                       << dst.policy << ";;" << tip_mask << endl;
            }
            out << "W1;" << endl;
        }
        pipette.src.volume = 0;
        pipette.dst_l.clear();
    }
}

void Backend::saveWorklistColor3() {
    //QFile file("C:\\Ellis\\openhouse.gwl");
    QFile file("openhouse.gwl");
    file.open(QIODevice::WriteOnly | QIODevice::Text);

    QTextStream out(&file);
    QVector<Pipette> pipette_l(8);
    for (int i = 0; i < pipette_l.size(); i++)
        pipette_l[i].src.tip = i;
    //QVector<RYBK> rybk_l(m_image.width() * m_image.height());
    QVector<QVector<SourceVolume>> sources_l(m_image.width() * m_image.height());
    //QVector<SourceVolume>> sources_l(m_image.width() * m_image.height());

    // Get RYBK quantites for each well
	for (int col = 0; col < m_image.width(); col++) {
		for (int row = 0; row < m_image.height(); row++) {
			const int i = col * m_image.height() + row;

			// Simplify the color, only use one of red, green, OR blue
			const QColor& color0 = m_image.pixel(col, row);
            const RYBK rybk = convertColor(color0);
            //rybk_l[i] = rybk;

            QVector<SourceVolume> sv_l = QVector<SourceVolume>(4);
            //qDebug() << "rybk" << rybk.R << rybk.Y << rybk.B << rybk.K;
            setupSourceVolumeColor(sv_l[0], rybk.R, 0);
            setupSourceVolumeColor(sv_l[1], rybk.Y, 1);
            setupSourceVolumeColor(sv_l[2], rybk.B, 2);
            setupSourceVolumeColor(sv_l[3], rybk.K, 3);
            sources_l[i] = sv_l;
            // FOR DEBUG ONLY
            /*if (col == 2) {
                QVector<int> v(4);
                for (int j = 0; j < 4; j++)
                    v[j] = sv_l[j].volume;
                qDebug() << col << row << v;
            }*/
        }
    }

    // Row sequence for 384 well plates, interlaced for greater efficiency
    const int rowSequence384[] = { 0,2,4,6, 1,3,5,7, 8,10,12,14, 9,11,13,15 };

    // Dispense water
    // Initialize the pipetting instructions
    for (int tip_i = 0; tip_i < 4; tip_i++) {
        pipette_l[tip_i].src.tip = tip_i;
        pipette_l[tip_i].src.plate = "System";
        pipette_l[tip_i].src.col = 2;
        pipette_l[tip_i].src.row = tip_i;
        pipette_l[tip_i].src.policy = "Water_A_1000";
        pipette_l[tip_i].src.volume = 0;
        pipette_l[tip_i].dst_l.clear();
    }
    for (int row0 = 0; row0 < m_image.height(); row0++) {
        // Following an interlaced row sequence for the 384 well plate
        int row = row0;
        const int tip_i = (row0 % 4);
        if (m_image.height() == 16) {
            row = rowSequence384[row0];
        }
        for (int col = 0; col < m_image.width(); col++) {
            const int i = col * m_image.height() + row;
            qreal volume = 0;
            const QVector<SourceVolume>& sources = sources_l[i];

            // Water
            for (auto sv : sources) {
                volume += sv.volume;
            }
            if (volume > 0 && volume < 25) {
                const qreal volumeWater = qMax(30 - volume, 5.0);
                qDebug() << row << col << volume << volumeWater;
                if (pipette_l[tip_i].src.volume + volumeWater > 900)
                    printWorklistPipetteItems(out, pipette_l);
                pipette_l[tip_i].src.volume += volumeWater;

                TipWellVolumePolicy dst;
                dst.tip = tip_i;
                dst.plate = "plate";
                dst.col = col;
                dst.row = row;
                dst.policy = "Water_A_1000";
                dst.volume = volumeWater;
                pipette_l[tip_i].dst_l += dst;
            }
        }
    }
    printWorklistPipetteItems(out, pipette_l);
    out << "B;" << endl;

    qDebug() << "volumes >= 3";
    // For each source, volumes >= 3
    for (int source_i = 0; source_i < 4; source_i++) {
        // Initialize the pipetting instructions
        for (int tip_i = 0; tip_i < 4; tip_i++) {
            pipette_l[tip_i].src.tip = tip_i;
            pipette_l[tip_i].src.plate = "T3";
            pipette_l[tip_i].src.col = 2;
            pipette_l[tip_i].src.row = source_i;
            pipette_l[tip_i].src.policy = "Water_A_1000";
            pipette_l[tip_i].src.volume = 0;
            pipette_l[tip_i].dst_l.clear();
        }

        // For each well
        for (int row0 = 0; row0 < m_image.height(); row0++) {
            // Following an interlaced row sequence for the 384 well plate
            int row = row0;
            if (m_image.height() == 16) {
                row = rowSequence384[row0];
            }
            for (int col = 0; col < m_image.width(); col++) {
                const int i = col * m_image.height() + row;
                const QVector<SourceVolume>& sources = sources_l[i];
                const SourceVolume& sv = sources[source_i];

                if (sv.volume >= 3) {
                    const int tip_i = (row % 4);
                    if (pipette_l[tip_i].src.volume + sv.volume > 900)
                        printWorklistPipetteItems(out, pipette_l);
                    pipette_l[tip_i].src.volume += sv.volume;

                    TipWellVolumePolicy dst;
                    dst.tip = tip_i;
                    dst.plate = "plate";
                    dst.col = col;
                    dst.row = row;
                    dst.policy = "Water_A_1000";
                    dst.volume = sv.volume;
                    pipette_l[tip_i].dst_l += dst;
                }
            }
        }
        printWorklistPipetteItems(out, pipette_l);
        out << "B;" << endl;
    }

    qDebug() << "volumes < 3";
    // For each source, volumes < 3
    for (int source_i = 0; source_i < 4; source_i++) {
        // Initialize the pipetting instructions
        for (int tip_i = 4; tip_i < 8; tip_i++) {
            pipette_l[tip_i].src.tip = tip_i;
            pipette_l[tip_i].src.plate = "T3";
            pipette_l[tip_i].src.col = 2;
            pipette_l[tip_i].src.row = source_i;
            pipette_l[tip_i].src.policy = "Water_C_50";
            pipette_l[tip_i].src.volume = 0;
            pipette_l[tip_i].dst_l.clear();
        }

        // For each well
        for (int row0 = 0; row0 < m_image.height(); row0++) {
            // Following an interlaced row sequence for the 384 well plate
            int row = row0;
            if (m_image.height() == 16) {
                row = rowSequence384[row0];
            }
            for (int col = 0; col < m_image.width(); col++) {
                const int i = col * m_image.height() + row;
                const QVector<SourceVolume>& sources = sources_l[i];
                const SourceVolume& sv = sources[source_i];

                if (sv.volume > 1 && sv.volume < 3) {
                    const int tip_i = (row % 4) + 4;
                    if (pipette_l[tip_i].src.volume > 0)
                        printWorklistPipetteItems(out, pipette_l);
                    pipette_l[tip_i].src.volume += sv.volume;

                    TipWellVolumePolicy dst;
                    dst.tip = tip_i;
                    dst.plate = "plate";
                    dst.col = col;
                    dst.row = row;
                    dst.policy = "Water_C_50";
                    dst.volume = sv.volume;
                    pipette_l[tip_i].dst_l += dst;
                }
            }
        }
        printWorklistPipetteItems(out, pipette_l);
    }
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
