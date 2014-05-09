#ifndef __BACKEND_H
#define __BACKEND_H

#include <QColor>
#include <QImage>
#include <QObject>
#include <QVector>


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

struct SourceVolume {
    QString plate;
    int row;
    int col;
    qreal volume;
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
    Q_INVOKABLE void saveWorklistSepia();
    Q_INVOKABLE void saveWorklistColor3();
	Q_INVOKABLE void saveWorklistColor3LargeTips();
	Q_INVOKABLE void colorizeMonochrome();
	Q_INVOKABLE void colorizeHues3();
	Q_INVOKABLE void colorizeHues6();

signals:
    void rowCountChanged(int);
    void colCountChanged(int);

public slots:

private:
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
