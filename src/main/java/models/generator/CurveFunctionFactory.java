package models.generator;

/**
 * Factory for curve functions
 * 
 * @author federico
 *
 */
public class CurveFunctionFactory {

	/**
	 * <ul>
	 * <li>#0 -> flat distribution
	 * <li>#1 -> monotonic decreasing linear distribution
	 * <li>#2 -> monotonic decreasing exponential distribution
	 * <li>#3 -> monotonic decreasing exponential distribution (steeper)
	 * </ul>
	 * 
	 * @author federico
	 *
	 */
	public enum CurveFunctionType {
		FLAT, LINEAR, EXP, EXP2
	}

	public static CurveFunction buildCurveFunction(CurveFunctionType type, int y0, int prodPages) {
		switch (type) {
		case FLAT:
			return new ConstantCurveFunction(y0, prodPages);
		case LINEAR:
			return new RationalCurveFunction(0.5, y0, prodPages);
		case EXP:
			return new RationalCurveFunction(0.1, y0, prodPages);
		case EXP2:
			return new RationalCurveFunction(0.01, y0, prodPages);
		default:
			throw new IllegalArgumentException("Unknown curve type");
		}
	}

	// TODO avoid this duplication

	public static CurveFunction buildCurveFunction(CurveFunctionType type, int y0, int x0, int minY) {
		switch (type) {
		case FLAT:
			return new ConstantCurveFunction(y0, x0, minY);
		case LINEAR:
			return new RationalCurveFunction(0.5, y0, x0, minY);
		case EXP:
			return new RationalCurveFunction(0.1, y0, x0, minY);
		case EXP2:
			return new RationalCurveFunction(0.01, y0, x0, minY);
		default:
			throw new IllegalArgumentException("Unknown curve type");
		}
	}

}
