#ifndef __BACKEND_H
#define __BACKEND_H

#include <QColor>
#include <QImage>
#include <QObject>
#include <QVector>


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

    qreal component(int index) const {
        switch (index) {
        case 0: return R;
        case 1: return Y;
        case 2: return B;
        case 3: return K;
        default: return 0;
        }
    }
};

struct TipWellVolumePolicy {
    int tip;
    QString plate;
    int row;
    int col;
    qreal volume;
    QString policy;

    TipWellVolumePolicy()
        : tip(0), row(0), col(0), volume(0.0)
    {}
};

struct Source {
    int sourceId;
    QString description;
    QString plate;
    int row;
    int col;
    RYBK conc; // RYBK concentration per ul

    Source(
        int sourceId,
        const QString& description,
        const QString& plate,
        int row,
        int col,
        const RYBK& conc
    ) : sourceId(sourceId), description(description), plate(plate), row(row), col(col), conc(conc)
    { }
};

struct SourceVolume {
    const Source* source;
    qreal volume;

    SourceVolume() : source(NULL), volume(0) {}
    SourceVolume(const Source* source, qreal volume) : source(source), volume(volume) {}
};

struct Pipette {
    TipWellVolumePolicy src;
    QVector<TipWellVolumePolicy> dst_l;
};

class Backend : public QObject
{
    Q_OBJECT
    Q_PROPERTY(int rowCount READ rowCount NOTIFY rowCountChanged)
    Q_PROPERTY(int colCount READ colCount NOTIFY colCountChanged)

public:
    explicit Backend(QObject *parent = 0);

    Q_INVOKABLE QString getFillStyle(const int row, const int col);
	int rowCount() const { return m_image.height(); }
	int colCount() const { return m_image.width(); }
    void setSize(int rowCount, int colCount);
    Q_INVOKABLE void setPlate(int plate_i);
    Q_INVOKABLE void setSize96();
    Q_INVOKABLE void setSize384();
    Q_INVOKABLE void setColor(const int row, const int col, const QString& colorName);
    Q_INVOKABLE void openImage(const QString& filename);
    Q_INVOKABLE void saveImage(const QString& filename);
    Q_INVOKABLE void saveWorklistColorchart384();
    Q_INVOKABLE void saveWorklistSepia();
    Q_INVOKABLE void saveWorklistColor3();
	Q_INVOKABLE void saveWorklistColor3LargeTips();
	Q_INVOKABLE void colorizeMonochrome();
	Q_INVOKABLE void colorizeHues3();
	Q_INVOKABLE void colorizeHues6();

    void saveWorklistColor4(const QVector<QVector<SourceVolume>>& wellToSources_ll);

signals:
    void rowCountChanged(int);
    void colCountChanged(int);

public slots:

private:
    QVector<RYBK> imageToRYBK() const;
    QVector<SourceVolume> rybkToSourceVolumes(const RYBK& rybk) const;
    QVector<QVector<SourceVolume>> rybkListToSourceVolumeList(const QVector<RYBK>& rybk_l) const;
    void printWorklistItems(
            class QTextStream& out,
            const int n,
            double aspirate[],
            const int tipWell[],
            const QString tipSite[],
            const QString tipLiquidClass[8],
            QString dispense[]);
    void printWorklistPipetteItems(
        QTextStream& out,
        QVector<Pipette>& pipette_l
    );

private:
	//QVector<QColor> color_l;
	QImage m_image;
	QImage m_image2;
    int m_rowCount;
    int m_colCount;
    int m_volumeSaturated;
    int m_volumeTotal;
};

#endif // __BACKEND_H
