#ifndef __BACKEND_H
#define __BACKEND_H

#include <QColor>
#include <QObject>
#include <QVector>

class Backend : public QObject
{
    Q_OBJECT
    Q_PROPERTY(int rowCount READ rowCount NOTIFY rowCountChanged)
    Q_PROPERTY(int colCount READ colCount NOTIFY colCountChanged)

public:
    explicit Backend(QObject *parent = 0);

    Q_INVOKABLE QString getFillStyle(const int row, const int col);
    int rowCount() const { return m_rowCount; }
    int colCount() const { return m_colCount; }
    void setSize(int rowCount, int colCount);
    Q_INVOKABLE void setPlate(int plate_i);
    Q_INVOKABLE void setSize96();
    Q_INVOKABLE void setSize384();
    Q_INVOKABLE void setColor(const int row, const int col, const QString& colorName);
    Q_INVOKABLE void openImage(const QString& filename);
    Q_INVOKABLE void saveImage(const QString& filename);
    Q_INVOKABLE void saveWorklist();


signals:
    void rowCountChanged(int);
    void colCountChanged(int);

public slots:

private:
    QVector<QColor> color_l;
    int m_rowCount;
    int m_colCount;
    int m_volume;
};

#endif // __BACKEND_H
