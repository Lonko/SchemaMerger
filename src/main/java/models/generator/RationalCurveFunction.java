package models.generator;

import java.util.Random;

public class RationalCurveFunction implements CurveFunction{
	
	/* curve types:
	 * 1 -> monotonic decreasing linear distribution
	 * 2 -> monotonic decreasing exponential distribution
	 * 3 -> monotonic decreasing exponential distribution (steeper)
	 */
	private String curveType;
	private int y0;
	private int x0;
	private int minY;
	private double alpha = 0.0;
	/* the values of f(x) for each integer 1 <= x <= x0
	 * each value y' is stored in the element in the x'-1 position
	 * (with x' being the x such that f(x') = y')
	 */
	private int[] yValues;

	//for curves whose parameters are all known
	public RationalCurveFunction(String type, int y0, int x0, int minY){
		this.curveType = type;
		this.y0 = y0;
		this.x0 = x0;
		this.minY = minY;
		this.yValues = new int[x0];
		if(type.equals("1"))
			this.alpha = 0.5;
		else if(type.equals("2"))
			this.alpha = 0.1;
		else if(type.equals("3"))
			this.alpha = 0.01;
		calculateYValues();
	}
	
	//for curves to be defined according to another curve's sampling
	public RationalCurveFunction(String type, int y0, int prodsPages){
		this.curveType = type;
		this.y0 = y0;
		this.minY = 1;
		if(type.equals("1"))
			this.alpha = 0.5;
		else if(type.equals("2"))
			this.alpha = 0.1;
		else if(type.equals("3"))
			this.alpha = 0.01;
		calculateYValues(prodsPages);
	}
	
	//returns y value for curve of type 1-2-3 (depending on the value of alpha)
	private int getRoundedYValue(int x){
		double numerator = ((1-this.alpha)*(x-1));
		double denominator = this.alpha*(this.x0-(x-1));
		double yValue = this.minY + (this.y0-this.minY)/(1+numerator/denominator);
		
		return (int) yValue;
	}
	
	/* calculates f(x) (as an int) for each x in [1,x0]
	 * for curves with a known x0
	 */
	private void calculateYValues(){
		for(int i=1; i<=this.x0; i++)
			this.yValues[i-1] += getRoundedYValue(i);
	}
	
	/* calculates f(x) (as an int) for each x in [1,x0]
	 * for curves without a known x0
	 */
	private void calculateYValues(int prodsPages){	 
		//binary search for an acceptable value of x0
		this.x0 = (prodsPages*2) / this.y0;
		int start = 1;
		int end = prodsPages;
		boolean foundX0 = false;
		do{
			this.yValues = new int[this.x0];
			calculateYValues();
			int integral = getSampling();
			int difference = integral - prodsPages;
			if(Math.abs(difference) < 0.01*prodsPages ){
				foundX0 = true;
				compensateError(difference);
			} else if(difference > 0){
				end = this.x0;
				this.x0 = (start + end) / 2; 
			} else {
				start = this.x0;
				this.x0 = (start + end) / 2;
			}
		} while(!foundX0);
	}
	
	private void compensateError(int error){
		boolean add = (error < 0);
		error = Math.abs(error);
		
		while(error > 0){
			int i = new Random().nextInt(this.x0);
			if(!(add && this.yValues[i]==this.y0) && !(!add && this.yValues[i]==this.minY)){
				this.yValues[i] += (add) ? 1 : -1;
				error--;
			}
		}
	}
	
	
	public int getSampling(){
		int integral = 0;
		for(int i=0; i<this.x0; i++)
			integral += this.yValues[i];
		
		return integral;
	}
	
	//finds x value that separates the head from the tail
	public int getHeadThreshold(){
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

	public int[] getYValues() {
		return yValues;
	}

	public String getCurveType() {
		return curveType;
	}
}
