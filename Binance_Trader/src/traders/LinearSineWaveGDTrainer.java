package traders;

import multithreading.NotifyingThread;

public class LinearSineWaveGDTrainer extends NotifyingThread {
	
	
	/* 
	 * This is the tolerance to which we minimize our error, in other words we will stop training when 
	 * | oldError - newError | < this value.  
	 * TODO: Empirically find a good value for this.
	 */ 
	private static double DONE_LEARNING = .001;
	
	// This is something that will stop the thread in the event of a "divergence" 
	private static final long THREAD_STOP_TIME_SEC = 30;
	
	/* 
	 * Not declaring as final because I may want to add something that adjusts the learning rate
	 * for better optimization.
	 */
	private double learningRate = .00001;
	
	// Here we will define a static variable that will contain the minimum error for the trainers
	private static volatile double minTrainingError = -1;
	// Another static variable that will contain the integer id of the trainer yielding the lowest error.
	private static volatile String bestTrainerName = null;
	// Finally, static double to hold the value of the best predicted price.
	private static volatile double bestPredictedPrice = -1;
	
	// Sine wave is of the form f(x) = a sin(bx + c) + d + ex. These are the constants we will try and learn.
	// These will be invisible to the user (for now, may want later). The only thing that will be visible will 
	// be the error and the predicted value.
	// TODO: Might be best to make this an array? 
	private double a = -1;
	private double b = -1;
	private double c = -1;
	private double d = -1;
	private double e = -1; //***
	
	// Current error, which the threads will be able to get. Note that we can make this volatile as the spawner 
	// of this class will be very interested in the value of this variable.
	private volatile double error = -1;
	// Current prediction value given a, b, c, d, and e
	private volatile double f_pred = -1;
	
	
	// Currently not used, but in theory will be the time that the lastRidge happened. Can be an int.
	private int lastRidge = -1;
	
	// The data we will try to fit to
	private double[] f = null;	
	
	// Boolean for knowing if we finished training.
	private volatile boolean finished = false;
	
	public LinearSineWaveGDTrainer(double a, double b, double c, double d, double e) {
		// Initial values of a, b, c, and d
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
	}
	
	public LinearSineWaveGDTrainer(double a, double b, double c, double d, double e, double[] f, int lastRidge) {
		this(a, b ,c, d, e);
		this.f = f;
		this.lastRidge = lastRidge;
	}
	

	@Override
	public void doRun() {
		// TODO Auto-generated method stub
		if (f != null && lastRidge != -1)
			trainModel(f, lastRidge);
	}
	
	/**
	 * This method will adjust a, b, c, d, and e to fit a periodic function defined by 
	 * f(x) = a * sin (b * x + c) + d + e * x using a simple gradient descent method. See
	 * the paper for details as to how the formulas were derived. 
	 * @param f - Specifies the double array with the last NUM_DATA price vals for the 
	 * market. 
	 * @param pts - Specifies the number of minutes/points since the last ridge. We will 
	 * only use the last [pts] points of f[].
	 */
	private void trainModel(double[] f, int pts) {
		finished = false;
		long startTime = System.nanoTime();
		// Before we do our fancy GD, we need to do our less fancy `find b by guess and check`
//		double MIN = -.3;
//		double MAX = .3;
//		int NUM_DATA = 200;
//		double INCREMENT = (MAX - MIN) / (NUM_DATA - 1);
//		double lowestError = -1;
//		double lowestYieldingB = -1;
//		for (double currentB = MIN; currentB <= MAX; currentB += INCREMENT) {
//			// Technically this won't calculate error, but will be a linear multiple of it (assuming pts is constant, which for testing it is.
//			double raw = 0; 
//			for (int i = f.length - pts; i < f.length; i++) {
//				raw += Math.pow((Math.sin(currentB * i) - f[i]), 2);
//			}
//			if (lowestError < 0 || raw < lowestError) {
//				lowestError = raw;
//				lowestYieldingB = currentB;
//			}
//
//		} 
//		// Finally, we have the b that is "close enough". So we set it to this.
//		b = lowestYieldingB;
		/*
		 * The general form for a parameter in gradient descent is
		 * a_{j+1} = a_j - r * D_j J(a) 
		 * where a_j is the j-th generation of an arbitrary parameter,
		 * r is the learning rate, D_j is the partial derivative with 
		 * respect to the j-th parameter (a_j), and J is the error function
		 */
		
		// The x value we use is the most recent price val.
		//double x = f[f.length - 1];
		boolean learning = true;
		// Initially give error an invalid value.
		double prevError = -1;
		while (learning && (System.nanoTime() - startTime < THREAD_STOP_TIME_SEC * 1e9)) {
//			if ((System.nanoTime() - startTime) / 1e9 > 30) {
//				System.out.println("UH-OH " + a + "sin( " + b + "x + " + c + " ) + " + d + " + " + e + "x");//***
//			}
			double sum_a = 0;
			double sum_b = 0;
			double sum_c = 0;
			double sum_d = 0;
			double sum_e = 0;//***
			double error_sum = 0;
			for (int i = f.length - pts; i < f.length; i++) {
				// We sum all the previous errors.
				int x_i = pts - f.length + i;
				double diff = h(x_i) - f[i];
				error_sum += diff * diff;
				sum_d += diff; 
				sum_a += diff * Math.sin(b * x_i);
				sum_b += diff * a * x_i * Math.cos(b * x_i + c);
				sum_c += diff * a * Math.cos(b * x_i + c);
				sum_e += diff * x_i;//***
			}
			// Don't forget to divide by pts at the end.
			sum_a /= (double) pts;
			sum_b /= (double) pts;
			sum_c /= (double) pts;
			sum_d /= (double) pts;
			sum_e /= (double) pts;//***
			// This is our error function (J(x))
			error = error_sum * 1/2 * pts ; 
			//addCSVEntry(error, 0d, 0d, 0d, "", f);
			/*if (error == 0) {
				System.out.println("No Error");
				break; // If there is no error, then the values are perfect, and we may exit.
			} else*/ if (Math.abs(error - prevError) < DONE_LEARNING && prevError >= 0) {
				learning = false;
			} else {
				prevError = error;
			}
			
			// Quick little check for setting the static min error.
			if (error < minTrainingError || minTrainingError < 0) {
				minTrainingError = error;
				bestTrainerName = this.getName();
			}
			
			/*
		 	*  Note that we can't yet assign these values to a, b, c, and d, as 
		 	*  this would screw up the other calculations, so we put them in 
		 	*  temporary variables for the time being.
		 	*/
			double temp_a = a - learningRate * 100 * sum_a;
			double temp_b = b - learningRate * .1 * sum_b; // Learning rate on this guy has to be very delicate as highly non-convex
			double temp_c = c - learningRate * 100 * sum_c;
			double temp_d = d - learningRate * 100 * sum_d;
			double temp_e = e - learningRate * 100 * sum_e;//***
		
			// Now we can set these.
			a = temp_a;
			b = temp_b;
			c = temp_c;
		    d = temp_d;
			e = temp_e;//***
			
			
		}
		//double time = (double)(System.nanoTime() - startTime) / 1e9;
		// Once we have finished training, we set f_pred to the proper thing.
		f_pred = h(lastRidge + 1);
		if (bestTrainerName.equals(this.getName())) {
			bestPredictedPrice = f_pred;
		}
		finished = true;
		//System.out.println(a + "sin( " + b + "x + " + c + " ) + " + d + " + " + e + "x   Error: " + error + "   Time: " + time + " seconds");//***
		//addCSVEntry(0d, 0d, 0d, 0d, "", f);
	}
	
	/**
	 * This method explicitly sets the data in case it was not specified initially.
	 * @param f - array of market prices over time
	 */
	public void setData(double[] f) {
		this.f = f;
	}
	
	/**
	 * This method  explicitly sets last ridge
	 * @param lastRidge - time of last ridge. Method in the Trader to get this. (May change tho)
	 */
	public void setLastRidge(int lastRidge) {
		this.lastRidge = lastRidge;
	}
	
	/**
	 * Hypothesis function, which is just a periodic function that 
	 * takes in an x, and returns a y, based on the current parameter.
	 * @param x - x value passed into function. To make numbers a little 
	 * nicer, we will make all the x's ints, and fit our curve to that.
	 */
	public double h (int x) { 
		return a * Math.sin(b * x + c) + d + e * x; //***
	}
	
	public boolean isfinished() {
		return finished;
	}
	
	public double getError() {
		return error;
	}
	
	// Static method to easily access the minimum training error
	public static double getMinError() {
		return minTrainingError;
	}
	
	// And for returning the thread name with lowest error
	public static String getBestThread() {
		return bestTrainerName;
	}
	
	// And last for returning the best predicted price
	public static double getBestPredictedPrice() {
		return bestPredictedPrice;
	}
	

}
