#include "backend.h"

#include <QFile>
#include <QImage>
#include <QTextStream>
#include <QUrl>
#include <QtDebug>


QVector<RYBK> colorInfo {
    RYBK(0, 0, 0, 0, QColor("#ffffff")), // white
    RYBK(0.827586, 0, 0, 0, QColor("#dc1424")), // red
    RYBK(0.413793, 0, 0, 0, QColor("#e7254a")), // red/2
    RYBK(0.206897, 0, 0, 0, QColor("#ff4e6b")), // red/4
    RYBK(0.103448, 0, 0, 0, QColor("#ff7990")), // red/8
    RYBK(0, 0.8, 0, 0, QColor("#ffe536")), // yellow
    RYBK(0, 0.4, 0, 0, QColor("#fff24f")), // yellow/2
    RYBK(0, 0.2, 0, 0, QColor("#fff569")), // yellow/4
    RYBK(0, 0.1, 0, 0, QColor("#fff792")), // yellow/8
    RYBK(0, 0, 1.2, 0, QColor("#0067a2")), // blue
    RYBK(0, 0, 0.6, 0, QColor("#059cc2")), // blue/2
    RYBK(0, 0, 0.3, 0, QColor("#2dc6d4")), // blue/4
    RYBK(0, 0, 0.15, 0, QColor("#58c9cb")), // blue/8
    RYBK(0, 0, 0, 0.510638, QColor("#000000")), // black
    RYBK(0, 0, 0, 0.255319, QColor("#5a5958")), // black/2
    RYBK(0, 0, 0, 0.12766, QColor("#797774")), // black/4
    RYBK(0, 0, 0, 0.0638298, QColor("#a0a0a0")), // black/8
    RYBK(0.0833333, 2.5, 0, 0, QColor("#ff5507")), // orange
    RYBK(0.0416667, 1.25, 0, 0, QColor("#ff8507")), // orange/2
    RYBK(0.0833333, 2.5, 0, 0.0583333, QColor("#cd5c0a")), // brown
    RYBK(0.0416667, 1.25, 0, 0.0291667, QColor("#e97637")), // brown/2
    RYBK(0.133333, 0, 0.258065, 0, QColor("#886fa6")), // purple
    RYBK(0, 0.4, 0.6, 0, QColor("#15ae27")), // green
    RYBK(0, 0.2, 0.3, 0, QColor("#4ece5d")), // green/2
    RYBK(0, 0.1, 0.15, 0, QColor("#6fe37c")), // green/4
};

QVector<const Source*> allSources_l {
    // RED
    new Source(11, "Red1", "Red1", 0, 1, RYBK(1.0/29, 0, 0, 0)),
    new Source(12, "Red2", "Red2", 0, 2, RYBK(1.0/232, 0, 0, 0)),
    //new Source(13, "Red3", "T3", 0, 3, RYBK(1.0/1856, 0, 0, 0)),

    // YELLOW
    new Source(21, "Yellow1", "Yellow1", 1, 1, RYBK(0, 1.0/30, 0, 0)),
    //new Source(22, "Yellow2", "T3", 1, 2, RYBK(0, 1.0/240, 0, 0)),
    new Source(24, "Yellow0", "T3", 1, 4, RYBK(0, 1.0/3, 0, 0)),

    // BLUE
    new Source(31, "Blue1", "Blue1", 2, 1, RYBK(0, 0, 1.0/20, 0)),
    //new Source(32, "Blue2", "T3", 2, 2, RYBK(0, 0, 1.0/160, 0)),

    // BLACK
    new Source(41, "Black1", "T2", 6, 0, RYBK(0, 0, 0, 1.0/47)),
    new Source(42, "Black2", "T2", 7, 0, RYBK(0, 0, 0, 1.0/376)),
    //new Source(43, "Black3", "T3", 3, 3, RYBK(0, 0, 0, 1.0/3008))
};


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

/*
QColor rgbToXyz(const QColor& color) {
    return QColor(
        color.redF() * 0.412453 + color.greenF() * 0.357580 + color.blueF() * 0.180423,
        color.redF() * 0.212671 + color.greenF() * 0.715160 + color.blueF() * 0.072169,
        color.redF() * 0.019334 + color.greenF() * 0.119193 + color.blueF() * 0.950227
    );
}

QColor xyzToLab(const QColor& color) {
    qreal L =
    return QColor(
        color.redF() * 0.412453 + color.greenF() * 0.357580 + color.blueF() * 0.180423,
        color.redF() * 0.212671 + color.greenF() * 0.715160 + color.blueF() * 0.072169,
        color.redF() * 0.019334 + color.greenF() * 0.119193 + color.blueF() * 0.950227
    );
}
*/

extern double deltaE2000( const QColor& bgr1, const QColor& bgr2 );
double deltaE1976(const QColor& color1, const QColor& color2);

qreal distDeltaE(const QColor& a, const QColor& b) {
    //return deltaE2000(a, b);
    return deltaE1976(a, b);
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

RYBK convertColorByDist(const QColor& color) {
    qreal dMin = distDeltaE(colorInfo[0].colorExpected, color);
    const RYBK* ci = &colorInfo[0];
    for (const RYBK& ci_ : colorInfo) {
        const qreal d = distDeltaE(ci_.colorExpected, color);
        //qDebug() << "distHue(" << ci_.colorExpected.hue() << ", " << hue << ") = " << d;
        if (d < dMin) {
            dMin = d;
            ci = &ci_;
        }
    }
    return *ci;
}

RYBK convertColorByHue(const QColor& color) {
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
    const RYBK rybk = convertColorByDist(color);
    return rybk.colorExpected;
}

QString toString(const RYBK& rybk) {
    return QString("RYBK(%1, %2, %3, %4)").arg(rybk.R).arg(rybk.Y).arg(rybk.B).arg(rybk.K);
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

/*
    auto rybk_l = imageToRYBK();
    auto wellToSources_ll = rybkListToSourceVolumeList(rybk_l);
*/
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

void Backend::saveWorklistColorchart384() {
    const qreal R = 24.0/29;
    const qreal Y = 24.0/30;
    const qreal B = 24.0/20;
    const qreal K = 24.0/47;
    QVector<RYBK> rybk_l(m_image.width() * m_image.height());

    qDebug() << "saveWorklistColorchart384" << "width:" << m_image.width() << "height:" << m_image.height();

    auto fn = [&] (int row, qreal amount) {
        for (int col = 0; col < 4; col++) {
            const int i = col * m_image.height() + row;
            const int factor = 1 << col;
            rybk_l[i].setComponent(row, amount / factor);
        }
    };

    fn(0, R);
    fn(1, Y);
    fn(2, B);
    fn(3, K);

    rybk_l[wellIndex(4, 0)] = RYBK(1.0/96*8, 30.0/96*8, 0, 0); // Orange
    rybk_l[wellIndex(4, 1)] = RYBK(1.0/96/2*8, 30.0/96/2*8, 0, 0); // Orange

    rybk_l[wellIndex(5, 0)] = RYBK(1.0/12, 30.0/12, 0, 0.7/12); // Brown
    rybk_l[wellIndex(5, 1)] = RYBK(1.0/12/2, 30.0/12/2, 0, 0.7/12/2); // Brown

    rybk_l[wellIndex(6, 0)] = RYBK(10.0/600*8, 0, 5.0/155*8, 0); // Purple

    rybk_l[wellIndex(7, 0)] = RYBK(0, 12.0/30, 12.0/20, 0); // Green
    rybk_l[wellIndex(7, 1)] = RYBK(0, 6.0/30, 6.0/20, 0); // Green
    rybk_l[wellIndex(7, 2)] = RYBK(0, 3.0/30, 3.0/20, 0); // Green
    //rybk_l[wellIndex(4, 2)] = RYBK(1.0/29, 30.0/30, 0, 0.7/96*8); // Brown

    auto wellToSources_ll = rybkListToSourceVolumeList(rybk_l);
    debug(rybk_l);
    debug(wellToSources_ll);
    saveWorklistColor4(wellToSources_ll);
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

QVector<RYBK> Backend::imageToRYBK() const {
    QVector<RYBK> rybk_l;

    for (int col = 0; col < m_image.width(); col++) {
        for (int row = 0; row < m_image.height(); row++) {
            // Simplify the color, only use one of red, green, OR blue
            const QColor& color0 = m_image.pixel(col, row);
            const RYBK rybk = convertColorByDist(color0);
            rybk_l += rybk;
        }
    }

    return rybk_l;
}

QVector<SourceVolume> Backend::rybkToSourceVolumes(const RYBK& rybk) const {
    QVector<SourceVolume> l;
    for (int i = 0; i < 4; i++) {
        if (rybk.component(i) > 0) {
            const Source* s = NULL;
            qreal v = 0;
            for (const Source* source : allSources_l) {
                if (source->conc.component(i) > 0) {
                    const qreal volume = round(rybk.component(i) / source->conc.component(i) * 10) / 10;
                    if (volume >= 3) {
                        if (s == NULL || volume < v) {
                            s = source;
                            v = volume;
                        }
                    }
                }
            }
            if (s != NULL) {
                l += SourceVolume(s, v);
            }
        }
    }

    for (auto x : l) {
        if (x.volume > 30) {
            qDebug() << "Large amount " << x.volume;
        }
    }

    return l;
}

QVector<QVector<SourceVolume>> Backend::rybkListToSourceVolumeList(const QVector<RYBK>& rybk_l) const {
    qDebug() << "rybkListToSourceVolumeList";
    QVector<QVector<SourceVolume>> ll;
    int i = 0;
    for (const RYBK& rybk : rybk_l) {
        auto l = rybkToSourceVolumes(rybk);
        for (auto sv : l) { qDebug() << i << sv.source->sourceId << sv.volume; }
        ll += l;
        i++;
    }
    return ll;
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

void Backend::saveWorklistColor4(const QVector<QVector<SourceVolume>>& wellToSources_ll) {
    QFile file("openhouse.gwl");
    file.open(QIODevice::WriteOnly | QIODevice::Text);

    QTextStream out(&file);
    QVector<Pipette> pipette_l(8);
    for (int i = 0; i < pipette_l.size(); i++)
        pipette_l[i].src.tip = i;

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
            const QVector<SourceVolume>& sources = wellToSources_ll[i];

            // Water
            qreal volume = 0;
            for (auto sv : sources) {
                volume += sv.volume;
            }
            if (volume > 0 && volume < 40) {
                const qreal volumeWater = 50 - volume;
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
    for (const Source* source : allSources_l) {
        // Initialize the pipetting instructions
        for (int tip_i = 0; tip_i < 4; tip_i++) {
            pipette_l[tip_i].src.tip = tip_i;
            pipette_l[tip_i].src.plate = source->plate;
            pipette_l[tip_i].src.col = source->col;
            pipette_l[tip_i].src.row = source->row;
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
                const QVector<SourceVolume>& sv_l = wellToSources_ll[i];

                for (const SourceVolume sv : sv_l) {
                    if (sv.source->sourceId == source->sourceId) {
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
            }
        }
        printWorklistPipetteItems(out, pipette_l);
        out << "B;" << endl;
    }

    qDebug() << "volumes < 3";
    // For each source, volumes < 3
    for (const Source* source : allSources_l) {
        // Initialize the pipetting instructions
        for (int tip_i = 4; tip_i < 8; tip_i++) {
            pipette_l[tip_i].src.tip = tip_i;
            pipette_l[tip_i].src.plate = source->plate;
            pipette_l[tip_i].src.col = source->col;
            pipette_l[tip_i].src.row = source->row;
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
                const QVector<SourceVolume>& sv_l = wellToSources_ll[i];

                for (const SourceVolume& sv : sv_l) {
                    if (sv.source->sourceId == source->sourceId) {
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
            }
        }
        printWorklistPipetteItems(out, pipette_l);
    }
}

void Backend::saveWorklistColor3() {
    auto rybk_l = imageToRYBK();
    auto wellToSources_ll = rybkListToSourceVolumeList(rybk_l);
    saveWorklistColor4(wellToSources_ll);
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

void Backend::debug(const QVector<RYBK>& l) {
    qDebug() << "[RYBK]";
    for (int col = 0; col < m_image.width(); col++) {
        for (int row = 0; row < m_image.height(); row++) {
            int i = col * m_image.height() + row;
            if (!l[i].isEmpty()) {
                qDebug() << "  row:" << row << "col:" << col << toString(l[i]);
            }
        }
    }
}

void Backend::debug(const QVector<SourceVolume>& l) {
    for (int col = 0; col < m_image.width(); col++) {
        for (int row = 0; row < m_image.height(); row++) {
            int i = col * m_image.height() + row;
            const SourceVolume& x = l[i];
            qDebug() << "  row:" << row << "col:" << col << x.source->description << x.volume;
        }
    }
}

void Backend::debug(const QVector<QVector<SourceVolume>>& ll) {
    qDebug() << "[[SourceVolume]]";
    for (int col = 0; col < m_image.width(); col++) {
        for (int row = 0; row < m_image.height(); row++) {
            int i = col * m_image.height() + row;
            const QVector<SourceVolume>& l = ll[i];
            if (!l.isEmpty()) {
                qDebug() << "  row:" << row << "col:" << col;
                for (const SourceVolume& x : l) {
                    qDebug() << "    " << x.source->description << x.volume;
                }
            }
        }
    }
}
