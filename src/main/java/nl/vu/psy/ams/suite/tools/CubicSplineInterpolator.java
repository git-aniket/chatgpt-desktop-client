package nl.vu.psy.ams.suite.tools;
/*
 * Cubic spline interpolator (from numerical recipes)
 * Used to interpolate the average HR in HRDrawer, and to
 * interpolate values in frequency analysis, when resampling
 * the ibis to a fixed grid.
 */
public class CubicSplineInterpolator {
	private double[]	y2;
	private double[]	x, y;
	private int			n;

	public CubicSplineInterpolator(double[] x, double[] y) {
		this.x = x;
		this.y = y;
		n = x.length;
		spline();
	}

	private void spline() {

		if (n < 2)
			return;
		double sig, p;
		y2 = new double[n];
		double[] u = new double[n];
		y2[0] = 0;
		u[0] = 0;
		for (int i = 1; i < n - 1; i++) {
			sig = (x[i] - x[i - 1]) / (x[i + 1] - x[i - 1]);
			p = sig * y2[i - 1] + 2;

			y2[i] = (sig - 1.0) / p;
			u[i] = (y[i + 1] - y[i]) / (x[i + 1] - x[i]) - (y[i] - y[i - 1]) / (x[i] - x[i - 1]);
			u[i] = (6 * u[i] / (x[i + 1] - x[i - 1]) - sig * u[i - 1]) / p;
		}
		double qn = 0;
		double un = 0;
		y2[n - 1] = (un - qn * u[n - 2]) / (qn * y2[n - 2] + 1);
		for (int k = n - 2; k >= 0; k--) {
			y2[k] = y2[k] * y2[k + 1] + u[k];
		}
	}

	public double splint(double xPos) {

		if (y == null || y2 == null)
			return 0;
		int klo, khi, k;
		double h, b, a;

		klo = 0;
		khi = n - 1;
		while (khi - klo > 1) {
			k = (khi + klo) >> 1;
			if (x[k] > xPos) {
				khi = k;
			} else {
				klo = k;
			}
		}
		h = x[khi] - x[klo];
		a = (x[khi] - xPos) / h;
		b = (xPos - x[klo]) / h;
		double ret = 0;
		if (klo < y.length && khi < y.length && klo < y2.length && khi < y2.length) {
			ret = a * y[klo] + b * y[khi] + ((a * a * a - a) * y2[klo] + (b * b * b - b) * y2[khi]) * (h * h) / 6.;
		}
		return ret;
	}

}
