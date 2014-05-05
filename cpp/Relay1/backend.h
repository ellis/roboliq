#ifndef __BACKEND_H
#define __BACKEND_H

#include <QColor>
#include <QObject>
#include <QVector>

class Backend : public QObject
{
    Q_OBJECT
public:
    explicit Backend(QObject *parent = 0);

    Q_INVOKABLE QString getFillStyle(const int row, const int col);
    Q_INVOKABLE void setColor(const int row, const int col, const QString& colorName);
    Q_INVOKABLE void openImage(const QString& filename);
    Q_INVOKABLE void saveImage(const QString& filename);
    Q_INVOKABLE void saveWorklist();

signals:

public slots:

private:
    QVector<QColor> color_l;
};

#endif // __BACKEND_H
