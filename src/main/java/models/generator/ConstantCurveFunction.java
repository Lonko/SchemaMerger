package models.generator;

import java.util.Random;

public class ConstantCurveFunction implements CurveFunction {

	private int y0;
	private int x0;
	/* the values of f(x) for each integer 1 <= x <= x0
	 * each value y' is stored in the element in the x'-1 position
	 * (with x' being the x such that f(x') = y')
	 */
	private int[] yValues;
	
	//for curves whose parameters are all known
	public ConstantCurveFunction(int y0, int x0, int minY){
		this.y0 = y0;
		this.x0 = x0;
		this.yValues = new int[x0];
		calculateYValues();
	}
	
	//for curves to be defined according to another curve's sampling
	public ConstantCurveFunction(int y0, int prodsPages){
		this.y0 = y0;
		calculateYValues(prodsPages);
	}
	
	/* calculates f(x) (as an int) for each x in [1,x0]
	 * for curves with a known x0
	 */
	private void calculateYValues(){
		for(int i=1; i<=this.x0; i++)
			this.yValues[i-1] += this.y0;
	}
	
	/* calculates f(x) (as an int) for each x in [1,x0]
	 * for curves without a known x0
	 */
	private void calculateYValues(int prodsPages){	
		this.x0 = (prodsPages+1) / ((this.y0)/2);
		this.yValues = new int[this.x0];
		calculateYValues();
		compensateError(getSampling()-prodsPages);
	}
	
	private void compensateError(int error){
		boolean add = (error < 0);
		error = Math.abs(error);
		
		while(error > 0){
			int i = new Random().nextInt(this.x0);
			this.yValues[i] += (add) ? 1 : -1;
			error--;
		}
	}
	
	@Override
	public int getSampling() {
		int integral = 0;
		for(int i=0; i<this.x0; i++)
			integral += this.yValues[i];
		
		return integral;
	}

	@Override
	public int getHeadThreshold() {
		int integral = getSampling();
		int head = 0;
		int x = this.x0;
		for(int i=1; i<=this.x0; i++){
			head += this.yValues[i-1];
			if(head >= (integral/2)){
				x = i;
				break;
			}
		}
		
		return x;
	}

	@Override
	public int[] getYValues() {
		return yValues;
	}

}
