//#include <opencv2/core/core.hpp>
//#include <opencv2/imgproc/imgproc.hpp>
//#include <opencv2/highgui/highgui.hpp>
//#include <opencv2/photo/photo.hpp>
#include <math.h>
#include <QColor>
#include <QVector3D>
#include <QtDebug>

//using namespace cv;
using namespace std;

#define CV_PI 3.14159265
#define REF_X 95.047; // Observer= 2°, Illuminant= D65
#define REF_Y 100.000;
#define REF_Z 108.883;

typedef QVector3D Vec3d;

void bgr2xyz( const QRgb& BGR, Vec3d& XYZ );
void xyz2lab( const Vec3d& XYZ, Vec3d& Lab );
void lab2lch( const Vec3d& Lab, Vec3d& LCH );
double deltaE2000( const QRgb& bgr1, const QRgb& bgr2 );
double deltaE2000( const Vec3d& lch1, const Vec3d& lch2 );


void bgr2xyz( const QRgb& BGR, Vec3d& XYZ )
{
    double r = (double)qRed(BGR) / 255.0;
    double g = (double)qGreen(BGR) / 255.0;
    double b = (double)qBlue(BGR) / 255.0;
    if( r > 0.04045 )
        r = pow( ( r + 0.055 ) / 1.055, 2.4 );
    else
        r = r / 12.92;
    if( g > 0.04045 )
        g = pow( ( g + 0.055 ) / 1.055, 2.4 );
    else
        g = g / 12.92;
    if( b > 0.04045 )
        b = pow( ( b + 0.055 ) / 1.055, 2.4 );
    else
        b = b / 12.92;
    r *= 100.0;
    g *= 100.0;
    b *= 100.0;
    XYZ[0] = r * 0.4124 + g * 0.3576 + b * 0.1805;
    XYZ[1] = r * 0.2126 + g * 0.7152 + b * 0.0722;
    XYZ[2] = r * 0.0193 + g * 0.1192 + b * 0.9505;
}

void xyz2lab( const Vec3d& XYZ, Vec3d& Lab )
{
    double x = XYZ[0] / REF_X;
    double y = XYZ[1] / REF_X;
    double z = XYZ[2] / REF_X;
    if( x > 0.008856 )
        x = pow( x , .3333333333 );
    else
        x = ( 7.787 * x ) + ( 16.0 / 116.0 );
    if( y > 0.008856 )
        y = pow( y , .3333333333 );
    else
        y = ( 7.787 * y ) + ( 16.0 / 116.0 );
    if( z > 0.008856 )
        z = pow( z , .3333333333 );
    else
        z = ( 7.787 * z ) + ( 16.0 / 116.0 );
    Lab[0] = ( 116.0 * y ) - 16.0;
    Lab[1] = 500.0 * ( x - y );
    Lab[2] = 200.0 * ( y - z );
}

void lab2lch( const Vec3d& Lab, Vec3d& LCH )
{
    LCH[0] = Lab[0];
    LCH[1] = sqrt( ( Lab[1] * Lab[1] ) + ( Lab[2] * Lab[2] ) );
    LCH[2] = atan2( Lab[2], Lab[1] );
}

double deltaE2000( const QColor& color1, const QColor& color2 )
{
    return deltaE2000(color1.rgb(), color2.rgb());
}

double deltaE2000( const QRgb& bgr1, const QRgb& bgr2 )
{
    Vec3d xyz1, xyz2, lab1, lab2, lch1, lch2;
    bgr2xyz( bgr1, xyz1 );
    bgr2xyz( bgr2, xyz2 );
    xyz2lab( xyz1, lab1 );
    xyz2lab( xyz2, lab2 );
    lab2lch( lab1, lch1 );
    lab2lch( lab2, lch2 );
    return deltaE2000( lch1, lch2 );
}

double deltaE1976(const QColor& color1, const QColor& color2) {
    Vec3d xyz1, xyz2, lab1, lab2;
    bgr2xyz(color1.rgb(), xyz1);
    bgr2xyz(color2.rgb(), xyz2);
    xyz2lab(xyz1, lab1);
    xyz2lab(xyz2, lab2);
    qreal L1 = lab1[0];
    qreal a1 = lab1[1];
    qreal b1 = lab1[2];
    qreal L2 = lab2[0];
    qreal a2 = lab2[1];
    qreal b2 = lab2[2];
    qreal deltaE = sqrt(pow(L2 - L1, 2) + pow(a2 - a1, 2) + pow(b2 - b1, 2));
    //qDebug() << color1 << color2 << xyz1 << xyz2 << lab1 << lab2 << deltaE;
    return deltaE;
}

double deltaE2000( const Vec3d& lch1, const Vec3d& lch2 )
{
    double avg_L = ( lch1[0] + lch2[0] ) * 0.5;
    double delta_L = lch2[0] - lch1[0];
    double avg_C = ( lch1[1] + lch2[1] ) * 0.5;
    double delta_C = lch1[1] - lch2[1];
    double avg_H = ( lch1[2] + lch2[2] ) * 0.5;
    if( fabs( lch1[2] - lch2[2] ) > CV_PI )
        avg_H += CV_PI;
    double delta_H = lch2[2] - lch1[2];
    if( fabs( delta_H ) > CV_PI )
    {
        if( lch2[2] <= lch1[2] )
            delta_H += CV_PI * 2.0;
        else
            delta_H -= CV_PI * 2.0;
    }

    delta_H = sqrt( lch1[1] * lch2[1] ) * sin( delta_H ) * 2.0;
    double T = 1.0 -
            0.17 * cos( avg_H - CV_PI / 6.0 ) +
            0.24 * cos( avg_H * 2.0 ) +
            0.32 * cos( avg_H * 3.0 + CV_PI / 30.0 ) -
            0.20 * cos( avg_H * 4.0 - CV_PI * 7.0 / 20.0 );
    double SL = avg_L - 50.0;
    SL *= SL;
    SL = SL * 0.015 / sqrt( SL + 20.0 ) + 1.0;
    double SC = avg_C * 0.045 + 1.0;
    double SH = avg_C * T * 0.015 + 1.0;
    double delta_Theta = avg_H / 25.0 - CV_PI * 11.0 / 180.0;
    delta_Theta = exp( delta_Theta * -delta_Theta ) * ( CV_PI / 6.0 );
    double RT = pow( avg_C, 7.0 );
    RT = sqrt( RT / ( RT + 6103515625.0 ) ) * sin( delta_Theta ) * -2.0; // 6103515625 = 25^7
    delta_L /= SL;
    delta_C /= SC;
    delta_H /= SH;
    return sqrt( delta_L * delta_L + delta_C * delta_C + delta_H * delta_H + RT * delta_C * delta_H );
}
