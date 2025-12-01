package nl.vu.psy.ams.suite.data.freq;

import java.util.HashMap;

import nl.vu.psy.ams.suite.main.AppSettings;
import nl.vu.psy.ams.suite.main.AppSettings.Settings;
import nl.vu.psy.ams.suite.tools.Timer;
import no.uib.cipr.matrix.BandLU;
import no.uib.cipr.matrix.BandMatrix;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;

public class SmoothnessPriorMatrices {

	private HashMap<Integer, Matrix> map = new HashMap<Integer, Matrix>();
	private static SmoothnessPriorMatrices instance;

	public static SmoothnessPriorMatrices getInstance() {
		if (instance == null)
			instance = new SmoothnessPriorMatrices();
		return instance;
	}

	private SmoothnessPriorMatrices() {
		reGenerate();
	}

	private void generate(int nMat) {

		double lambda = AppSettings.getInstance().getIntPropertyOrToBeSaved(Settings.FREQLAMBDA) / 100.;

		Timer tim = new Timer();

		double l2 = lambda * lambda;

		tim.start();
		BandMatrix bm = new BandMatrix(nMat, 2, 2);
		bm.zero();

		for (int i = 2; i < nMat - 2; i++) {
			bm.set(i, i - 2, l2);
			bm.set(i, i - 1, -4 * l2);
			bm.set(i, i, 6 * l2 + 1);
			bm.set(i, i + 1, -4 * l2);
			bm.set(i, i + 2, 1 * l2);
		}
		bm.set(0, 0, 1 * l2 + 1);
		bm.set(0, 1, -2 * l2);
		bm.set(0, 2, 1 * l2);
		bm.set(1, 0, -2 * l2);
		bm.set(1, 1, 5 * l2 + 1);
		bm.set(1, 2, -4 * l2);
		bm.set(1, 3, 1 * l2);

		bm.set(nMat - 1, nMat - 1, 1 * l2 + 1);
		bm.set(nMat - 1, nMat - 2, -2 * l2);
		bm.set(nMat - 1, nMat - 3, 1 * l2);
		bm.set(nMat - 2, nMat - 1, -2 * l2);
		bm.set(nMat - 2, nMat - 2, 5 * l2 + 1);
		bm.set(nMat - 2, nMat - 3, -4 * l2);
		bm.set(nMat - 2, nMat - 4, 1 * l2);
		tim.stop();
		// if(AppSettings.getInstance().getIntProperty(Settings.DEBUG)==1)
		// System.out.println("SmoothnessPriorMatrices1 took (" + tim.getTime() / 1000.
		// + " sec)");

		tim.start();
		BandLU blu = BandLU.factorize(bm);
		Matrix invT = blu.solve(Matrices.identity(nMat));
		tim.stop();
		// if(AppSettings.getInstance().getIntProperty(Settings.DEBUG)==1)
		// System.out.println("SmoothnessPriorMatrices2 took (" + tim.getTime() / 1000.
		// + " sec)");

		invT = invT.scale(-1);

		for (int i = 0; i < nMat; i++) {
			invT.add(i, i, 1);
		}

		map.put(nMat, invT);
	}

	public Matrix getSmoothnessPriorMatrix(int dim) {
		return map.get(dim);
	}

	public void reGenerate() {
		for (int i = 4; i <= 1024; i *= 2)
			generate(i);
		generate(720);
		generate(230);
	}
}
