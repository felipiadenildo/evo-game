package game.evo.utils;

/**
 * OpenSimplex Noise in Java.
 * by Kurt Spencer
 *
 * This is a direct, corrected copy of the commonly used single-file implementation.
 * This version is known to be functional and contains the necessary constructor and .eval() method.
 */
public class OpenSimplexNoise {

	private static final double STRETCH_CONSTANT_2D = -0.211324865405187;    //(1/Math.sqrt(2+1)-1)/2;
	private static final double SQUISH_CONSTANT_2D = 0.366025403784439;      //(Math.sqrt(2+1)-1)/2;
	private static final double STRETCH_CONSTANT_3D = -1.0 / 6.0;            //(1/Math.sqrt(3+1)-1)/3;
	private static final double SQUISH_CONSTANT_3D = 1.0 / 3.0;              //(Math.sqrt(3+1)-1)/3;
	private static final double STRETCH_CONSTANT_4D = -0.138196601125011;    //(1/Math.sqrt(4+1)-1)/4;
	private static final double SQUISH_CONSTANT_4D = 0.309016994374947;      //(Math.sqrt(4+1)-1)/4;

	private static final double NORM_CONSTANT_2D = 47.0;
	private static final double NORM_CONSTANT_3D = 103.0;
	private static final double NORM_CONSTANT_4D = 30.0;

	private static final long DEFAULT_SEED = 0;

	private short[] perm;
	private short[] permGradIndex3D;

	public OpenSimplexNoise() {
		this(DEFAULT_SEED);
	}

	public OpenSimplexNoise(short[] p) {
		perm = p;
		permGradIndex3D = new short[256];

		for (int i = 0; i < 256; i++) {
			permGradIndex3D[i] = (short)((perm[i] % (gradients3D.length / 3)) * 3);
		}
	}

	public OpenSimplexNoise(long seed) {
		perm = new short[256];
		permGradIndex3D = new short[256];
		short[] source = new short[256];
		for (short i = 0; i < 256; i++)
			source[i] = i;
		seed = seed * 6364136223846793005l + 1442695040888963407l;
		seed = seed * 6364136223846793005l + 1442695040888963407l;
		seed = seed * 6364136223846793005l + 1442695040888963407l;
		for (int i = 255; i >= 0; i--) {
			seed = seed * 6364136223846793005l + 1442695040888963407l;
			int r = (int)((seed + 31) % (i + 1));
			if (r < 0)
				r += (i + 1);
			perm[i] = source[r];
			permGradIndex3D[i] = (short)((perm[i] % (gradients3D.length / 3)) * 3);
			source[r] = source[i];
		}
	}

	// 2D OpenSimplex Noise.
	public double eval(double x, double y) {

		// Place input coordinates onto grid.
		double stretchOffset = (x + y) * STRETCH_CONSTANT_2D;
		double xs = x + stretchOffset;
		double ys = y + stretchOffset;

		// Floor to get grid coordinates of rhombus (stretched square) cell origin.
		int xsb = fastFloor(xs);
		int ysb = fastFloor(ys);

		// Skew out to get coordinates of cell origin in unstretched space.
		double squishOffset = (xsb + ysb) * SQUISH_CONSTANT_2D;
		double xb = xsb + squishOffset;
		double yb = ysb + squishOffset;

		// Compute grid coordinates relative to rhombus origin.
		double xins = xs - xsb;
		double yins = ys - ysb;

		// Sum those together to get a value that determines which region we're in.
		double inSum = xins + yins;

		// Positions relative to origin point.
		double dx0 = x - xb;
		double dy0 = y - yb;

		// --- *** CORREÇÃO PRINCIPAL AQUI *** ---
		// A variável 'value' deve ser inicializada no início do método.
		double value = 0;
		// --- *** FIM DA CORREÇÃO *** ---

		// Contribution (1,0)
		double dx1 = dx0 - 1 - SQUISH_CONSTANT_2D;
		double dy1 = dy0 - 0 - SQUISH_CONSTANT_2D;
		double attn1 = 2 - dx1 * dx1 - dy1 * dy1;
		if (attn1 > 0) {
			attn1 *= attn1;
			value += attn1 * attn1 * extrapolate(xsb + 1, ysb + 0, dx1, dy1);
		}

		// Contribution (0,1)
		double dx2 = dx0 - 0 - SQUISH_CONSTANT_2D;
		double dy2 = dy0 - 1 - SQUISH_CONSTANT_2D;
		double attn2 = 2 - dx2 * dx2 - dy2 * dy2;
		if (attn2 > 0) {
			attn2 *= attn2;
			value += attn2 * attn2 * extrapolate(xsb + 0, ysb + 1, dx2, dy2);
		}
		
		int xsv_ext;
		int ysv_ext;
		double dx_ext;
		double dy_ext;

		if (inSum <= 1) { // We're inside the triangle (2-Simplex) at (0,0)
			double zins = 1 - inSum;
			if (zins > xins || zins > yins) { // (0,0) is one of the closest two triangular vertices
				if (xins > yins) {
					xsv_ext = xsb + 1;
					ysv_ext = ysb - 1;
					dx_ext = dx0 - 1;
					dy_ext = dy0 + 1;
				} else {
					xsv_ext = xsb - 1;
					ysv_ext = ysb + 1;
					dx_ext = dx0 + 1;
					dy_ext = dy0 - 1;
				}
			} else { // (1,1) is the closest vertex
				xsv_ext = xsb + 1;
				ysv_ext = ysb + 1;
				dx_ext = dx0 - 1 - 2 * SQUISH_CONSTANT_2D;
				dy_ext = dy0 - 1 - 2 * SQUISH_CONSTANT_2D;
			}
		} else { // We're inside the triangle (2-Simplex) at (1,1)
			double zins = 2 - inSum;
			if (zins < xins || zins < yins) { // (1,1) is one of the closest two triangular vertices
				if (xins > yins) {
					xsv_ext = xsb + 2;
					ysv_ext = ysb + 0;
					dx_ext = dx0 - 2 - 2 * SQUISH_CONSTANT_2D;
					dy_ext = dy0 - 0 - 2 * SQUISH_CONSTANT_2D;
				} else {
					xsv_ext = xsb + 0;
					ysv_ext = ysb + 2;
					dx_ext = dx0 - 0 - 2 * SQUISH_CONSTANT_2D;
					dy_ext = dy0 - 2 - 2 * SQUISH_CONSTANT_2D;
				}
			} else { // (0,0) is the closest vertex
				xsv_ext = xsb;
				ysv_ext = ysb;
				dx_ext = dx0;
				dy_ext = dy0;
			}
			xsb += 1;
			ysb += 1;
			dx0 = dx0 - 1 - 2 * SQUISH_CONSTANT_2D;
			dy0 = dy0 - 1 - 2 * SQUISH_CONSTANT_2D;
		}
		
		// Contribution (0,0) or (1,1)
		double attn0 = 2 - dx0 * dx0 - dy0 * dy0;
		if (attn0 > 0) {
			attn0 *= attn0;
			value += attn0 * attn0 * extrapolate(xsb, ysb, dx0, dy0);
		}
		
		// Extra contribution
		double attn_ext = 2 - dx_ext * dx_ext - dy_ext * dy_ext;
		if (attn_ext > 0) {
			attn_ext *= attn_ext;
			value += attn_ext * attn_ext * extrapolate(xsv_ext, ysv_ext, dx_ext, dy_ext);
		}

		return value / NORM_CONSTANT_2D;
	}

	private double extrapolate(int xsb, int ysb, double dx, double dy) {
		int index = perm[(perm[xsb & 0xFF] + ysb) & 0xFF] & 0x0E;
		return gradients2D[index] * dx
			+ gradients2D[index + 1] * dy;
	}

	private static int fastFloor(double x) {
		int i = (int)x;
		return x < i ? i - 1 : i;
	}
	
	private static byte[] gradients2D = new byte[] {
		 5,  2,    2,  5,
		-5,  2,   -2,  5,
		 5, -2,    2, -5,
		-5, -2,   -2, -5,
	};
	
	private static byte[] gradients3D = new byte[] {
		-11,  4,  4,     -4,  11,  4,    -4,  4,  11,
		 11,  4,  4,      4,  11,  4,     4,  4,  11,
		-11, -4,  4,     -4, -11,  4,    -4, -4,  11,
		 11, -4,  4,      4, -11,  4,     4, -4,  11,
		-11,  4, -4,     -4,  11, -4,    -4,  4, -11,
		 11,  4, -4,      4,  11, -4,     4,  4, -11,
		-11, -4, -4,     -4, -11, -4,    -4, -4, -11,
		 11, -4, -4,      4, -11, -4,     4, -4, -11,
	};
}