package Spares_Algorithm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class F_Spares_Algorithm {

	//FILE LOCATIONS AND NAMES
	final static String directory = "C:\\data analysis\\project\\";
	final static String lru_file_name = "DPL_LRU.csv";
	final static String sas_file_name = "DPL_SA.csv";
	final static String part_data_file_name = "part_data.csv";
	//PRINT WRITERS USED TO GENERATE REPORTS
	static PrintWriter convergence;// = new PrintWriter(new File(directory + "convergence.csv"));
	static PrintWriter data_used;// = new PrintWriter(new File(directory + "data_used.csv"));
	static PrintWriter obj_stat;// = new PrintWriter(new File(directory + "obj_fun_stats.csv"));
	static PrintWriter best_solution;// = new PrintWriter(new File(directory + "obj_fun_stats.csv"));
	
	static //PARAMETERS
	int N_Pop = 90; //SIZE OF SOLUTION POPULATION
	static int N_Rand = 10; //THIS IS THE NUMBER OF RANDOM INDIVIDUALS TO CREATE AT START OF EACH ITERATION
	static int N_fractals = 4; //THIS IS THE NUMBER OF NEIGHBORS TO GENERATE FOR EACH POPULATION MEMBER AFTER RANDOM GENERATIONS
	static int Ini_max_spare = 100; //MAXIMUM NUMBER OF SPARES INITIALLY STORED IN SOLUTION
	static int N_Iterations = 4; //NUMBER OF TIMES THAT THE OBJECTIVE FUNCTION RUNS SIMULATION FOR EACH SOLUTION
	static int Quarters = 12; //NUMBER OF QUARTERS TO RUN OBJECTIVE FUNCTION SIMULATION FOR (BASED ON THREE YEARS, NEW FLEET & MODEL)
	static double lt_penalty = 40000000/(15*365); //LOST PROFITS BASED ON COST OF AIRCRAFT OVER ESTIMATED SERVICE YEARS
	static double c_penalty = .12/4; //THE COST FOR A SPARES COULD OTHERWISE HAVE BEEN SPENT ON OTHER BUSINESS VENTURES AT 12% ANNUAL RETURN
	static int max_generations_no_improvement = 100; //END PROCESS IF THIS MANY ITERATIONS PROVIDE NO REAL IMPROVEMENT TO AVERAGE OR BEST SOLUTION
	static double mating_fraction = .1; //ONLY THIS FRACTION OF TOTAL POPULATION MATES SUCCESFULLY
	
	
	///BELOW ARE BUCKETS, NOT PARAMETERS... THESE CONTAIN RAW DATA FOR PROGRAM TO FUNCTION
	//[NHA][ELEMENT][QTY]	//AS APPEARS IN RAW DATA
	static ArrayList<ArrayList<Integer>> lrus_brkdwns = new ArrayList<ArrayList<Integer>>();
	static ArrayList<ArrayList<Integer>> sas_brkdwns = new ArrayList<ArrayList<Integer>>();
	//[PART][MBTF][COST][REPARABLE][LEAD TIME]
	static ArrayList<double[]> part_data = new ArrayList<double[]>();
	 //THIS WILL HOLD ALL DATA FOR BREAKDOWNS
	////[MBTF][COST][REPARABLE][LEAD TIME]
	static ArrayList<ArrayList<double[]>> d_brkdwns = new ArrayList<ArrayList<double[]>>();
	////[PN][QTY]
	static ArrayList<ArrayList<int[]>> brkdwns = new ArrayList<ArrayList<int[]>>();
	
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		
		//SET UP PRINT WRITERS
		convergence = new PrintWriter(new File(directory + "convergence.csv"));
		data_used = new PrintWriter(new File(directory + "data_used.csv"));
		obj_stat = new PrintWriter(new File(directory + "obj_fun_stats.csv"));
		best_solution = new PrintWriter(new File(directory + "best_solution.csv"));
		
		///THIS WILL LOAD DATA INTO THE GLOBAL CONTAINERS AVAILABLE, SPECIFIED AT TOP OF CLASS
		load_data();
		
		///GENERATE INITIAL POPULATION RANDOMLY FROM LISTS OF ITEMS USED ON FLEET
		ArrayList<ArrayList<Integer>> X_Pop = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i < N_Pop; i++){
			ArrayList<Integer> x = new ArrayList<Integer>();
			int n_spares = (int) Math.round(Math.random()*Ini_max_spare);
			for(int j = 0; j < n_spares; j++){
				//DECIDE IF WILL STOCK LRU/SAS PART OR BREAKDOWN
				double dec = Math.random();
				if(dec < .5){
					//LRU
					int index = (int)Math.round(Math.random()*(lrus_brkdwns.size()-1));
					if(lrus_brkdwns.get(index).get(1) != null){
						x.add(lrus_brkdwns.get(index).get(1));						
					}else{
						x.add(lrus_brkdwns.get(index).get(0));
					}
				}else{
					//SAS
					int index = (int)Math.round(Math.random()*(sas_brkdwns.size()-1));
					if(sas_brkdwns.get(index).get(1) != null){
						x.add(sas_brkdwns.get(index).get(1));						
					}else{
						x.add(sas_brkdwns.get(index).get(0));
					}
				}
			}
			X_Pop.add(x);
		}
	
		//TEST POPULATION
		/*
		for(int i = 0; i < X_Pop.size(); i++){
			System.out.println();
			for(int j = 0; j < X_Pop.get(i).size(); j++){
				System.out.print(X_Pop.get(i).get(j)+"\t");
			}
		}
		*/
		
		///
		//GA SELECTION METHODOLOGY WITH A RANDOM SOLUTION AND VARIABLE NEIGHBOR DIVERSIFICATION
		ArrayList<Integer> solution = new ArrayList<Integer>();
		boolean solution_is_improving = true;
		int stalled_count = 0;
		int iteration = 0;
		double last_average_health = 0;
		while(solution_is_improving){
			iteration++;
			///
			//EXPAND SOLUTION WITH RANDOM POINTS FOR DIVERIFICATION
			for(int rp = 0; rp < N_Rand; rp++){
				X_Pop.add(random_solution());
			}
			//EXPAND SOLUTION WITH FRACTALS (RANDOMLY SELECTED NEIGHBORS OF PRE-EXISITING POINTS)
			for(int fp = 0; fp < N_Rand + N_Pop; fp++){
				ArrayList<Integer> x = X_Pop.get(fp);
				ArrayList<Integer> xrandom = random_solution();
				for(int ff = 0; ff < N_fractals; ff++){
					int modifyThis = (int)Math.round(Math.random()*(x.size()-1));
					if(xrandom.size()>ff){
						if(x.size() < 10){
							x.add(xrandom.get(ff));
						}else{
							x.set(modifyThis, xrandom.get(ff));
						}
					}else{
						if(x.size() < 10){
							x.add(xrandom.get(0));
						}else{
							x.set(modifyThis, xrandom.get(0));
						}
					}
					X_Pop.add(x);
				}
			}
			
			//SURVIVAL OF EXISTING POPULATION & MATING
			//FIND AVERAGE GOLF FITNESS (LESS IS BEST)
			double average_health = 0;
			double best_health = 500000000;
			for(int i = 0; i < N_Pop; i++){
				double health = obj_fun(X_Pop.get(i));
				average_health = average_health + health/N_Pop;
				if(health < best_health){
					best_health = health;
					solution = X_Pop.get(i);
				}
			}
			
			//DETERMINE IF NEW SOLUTION IS STALLED THROUGH AVERAGE HEALTH
			if(average_health - last_average_health >= -.05 ){
				stalled_count++;
			}else{
				stalled_count = 0;
			}
			if(stalled_count > max_generations_no_improvement){
				solution_is_improving = false;
			}
			last_average_health = average_health; //CARRY OVER LAST AVERAGE FOR NEXT COMPARISON
			
			//PRINT OFF PROGRESS
			System.out.println(average_health+"\t\t"+best_health);
			convergence.println(iteration+","+average_health+","+best_health);
			
			//ROULETTE TABLE DETERMINES CHANCE OF COPULATING
			//IF GREATER THAN 1.5AVERAGE: 1 SPOT
			//ELSE IF GREATER THAN AVERAGE: 2 SPOTS
			//ELSE IF GREATER THAN HALF OF AVERAGE: 4 SPOTS
			//ELSE: 8 SPOTS
			//int number_of_spots = 0;
			//int[] pop_x_mate_chance = new int[pop_size];
			ArrayList<Integer> roulette_wheel = new ArrayList<Integer>();
			for(int i = 0; i < (N_Pop + N_Rand)* N_fractals; i++){
				double fitness = obj_fun(X_Pop.get(i));
				if(fitness > 1.5 * average_health){
					//number_of_spots = number_of_spots + 1;
					//pop_x_mate_chance[i] = 1;
					//roulette_wheel.add(i);
				}else if(fitness > average_health){
					//number_of_spots = number_of_spots + 2;
					//pop_x_mate_chance[i] = 2;
					//roulette_wheel.add(i);
					roulette_wheel.add(i);
				}else if(fitness > .5 * average_health){
					//number_of_spots = number_of_spots + 4;
					//pop_x_mate_chance[i] = 4;
					roulette_wheel.add(i);
					roulette_wheel.add(i);
					roulette_wheel.add(i);
					roulette_wheel.add(i);
				}else if(fitness > 1.5 * average_health){
					//number_of_spots = number_of_spots + 8;
					//pop_x_mate_chance[i] = 8;
					roulette_wheel.add(i);
					roulette_wheel.add(i);
					roulette_wheel.add(i);
					roulette_wheel.add(i);
					roulette_wheel.add(i);
					roulette_wheel.add(i);
					roulette_wheel.add(i);
					roulette_wheel.add(i);
				}
			}
			//DETERMINE POSITION OF ROULLETE SELECTION POINTS
			//ALL OF THE POTENTIAL SOLUTIONS ARE ALLOWED TO TRY AND MATE, EVEN THE NEW COMERS FROM RANDOM AND FRACTALS GENERATION
			int number_of_maters = (int)Math.round(mating_fraction * (N_Pop + N_Rand)* N_fractals);
			int space_between_maters = (int)Math.round(roulette_wheel.size()/number_of_maters);
			//SPIN THE WHEEL
			int first_mater = (int)Math.round(Math.random()*roulette_wheel.size());
			int[] maters_by_index = new int[number_of_maters];
			
			int current_mater = first_mater;
			for(int i = 0; i < number_of_maters; i++){
				if(current_mater >= roulette_wheel.size()){
					//LAP THE WHEEL. THE ARRAY IS JUST A STRAIGHT LINE OTHERWISE
					current_mater = current_mater - roulette_wheel.size() + 0;
				}
				maters_by_index[i] = roulette_wheel.get(current_mater);
				current_mater = current_mater + space_between_maters;
			}
			
			//PRINT OFF THE LUCKY INDIVIDUALS SELECTED TO MATE
			//for(int i = 0; i < maters_by_index.length; i++){
			//	System.out.println(maters_by_index[i]);
			//}
			
			//GENERATE NEXT POPULATION BASED OFF OF CHANCE MUTATIONS AND PREVIOUS PARENTS
			//EACH TWO PARENTS WILL HAVE 2 * (1/MATING_FRACTION) CHILDREN, TO REPLACE PREVIOUS POPULATION
			//PARENT i MATES WITH PARENT n - i
			//RANDOMLY SELECT WHICH PARENT PROVIDES CHROMOSOME
			
			//int[][][] new_population_x = new int[N_Pop][n][n];
			ArrayList<ArrayList<Integer>> new_population_x = new ArrayList<ArrayList<Integer>>();
			
			int kid_number = 0; //KEEPS TRACK OF NEW KID GOING INTO NEW POPULATION
			for(int i = 0; i < number_of_maters; i++){
				ArrayList<Integer> parent_xy = X_Pop.get(maters_by_index[i]);
				ArrayList<Integer> parent_xx = X_Pop.get(maters_by_index[maters_by_index.length-i-1]);
				
				//EACH GROUPING WILL PRODUCE MULTIPLE CHILDREN
				for(int virility = 0; virility < N_Pop/number_of_maters; virility++){
					
					ArrayList<Integer> kid_under_construction = new ArrayList<Integer>();
					///PROVIDE FOUR RANDOM POSITIONS FOR BISECTION CUT
					double pos1 = Math.random()/4;
					double pos2 = (1- pos1)*Math.random()/3;
					double pos3 = (1- pos2 - pos1)*Math.random()/2;
					double pos4 = (1- pos2- pos1 - pos3)*Math.random();
					//CUT POINTS:
					int length = Math.min(parent_xy.size(),parent_xx.size());
					int cut1 = (int)Math.round(pos1*length);
					int cut2 = (int)Math.round(pos2*length) + cut1;
					int cut3 = (int)Math.round(pos3*length) + cut2;
					int cut4 = (int)Math.round(pos4*length) + cut3;
					
					//FOR EACH CUT, RANDOMLY SELECT PARENT TO PROVIDE MATERIAL, AND INSERT MATERIAL INTO SPAWN
					ArrayList<Integer> donor1;
					ArrayList<Integer> donor2;
					if(Math.random() < .5){
						donor1 = parent_xy;
						donor2 = parent_xx;
					}else{
						donor1 = parent_xx;
						donor2 = parent_xy;
					}
					
					for(int c = 0; c < donor2.size(); c++){
						if(c < cut1){
							kid_under_construction.add(donor1.get(c));
						}else if(c < cut2){
							kid_under_construction.add(donor2.get(c));
						}else if(c < cut3){
							kid_under_construction.add(donor1.get(c));
						}else{
							kid_under_construction.add(donor2.get(c));
						}
					}
					
					//THIS KID IS COMPLETE, FILE INTO NEW POPULATION
					new_population_x.add(kid_under_construction);
					kid_number++;
				}
			}
			
			//SAVE NEW POPULATION
			X_Pop = new_population_x;			
	
		}
		
		for(int i = 0; i < solution.size(); i++){
			best_solution.println(solution.get(i));
		}
		
		best_solution.close();
		convergence.close();
		obj_stat.close();
		
		
	}
	
	public static ArrayList<Integer> random_solution(){
		ArrayList<Integer> x = new ArrayList<Integer>();
		int n_spares = (int) Math.round(Math.random()*Ini_max_spare) + 10;
		for(int j = 0; j < n_spares; j++){
			//DECIDE IF WILL STOCK LRU/SAS PART OR BREAKDOWN
			double dec = Math.random();
			if(dec < .5){
				//LRU
				int index = (int)Math.round(Math.random()*(lrus_brkdwns.size()-1));
				if(lrus_brkdwns.get(index).get(1) != null){
					x.add(lrus_brkdwns.get(index).get(1));						
				}else{
					x.add(lrus_brkdwns.get(index).get(0));
				}
			}else{
				//SAS
				int index = (int)Math.round(Math.random()*(sas_brkdwns.size()-1));
				if(sas_brkdwns.get(index).get(1) != null){
					x.add(sas_brkdwns.get(index).get(1));						
				}else{
					x.add(sas_brkdwns.get(index).get(0));
				}
			}
		}
		
		return x;

	}
	
	//OBJECTIVE FUNCTION
	//BASED ON SPARES COST AND FLEET DOWNTIME, FIND THE DOLLAR COST OF SPARES OPERATIONS
	//OVER SPAN OF THREE YEARS WITH QUARTERLY ITERATIONS
	public static double obj_fun(ArrayList<Integer> x){
		
		double average_cost = 0;
		
		//FOR EACH ITERATION
		for(int i = 0; i < N_Iterations; i++){
			obj_stat.print("Iteration #"+i+",");
			double cost = 0;
			//FOR EACH QUARTER
			for(int q = 1; q <= Quarters; q++){
				//CREATE WORKING COPY OF SOLUTION, WILL HAVE PARTS REMOVED FROM IT
				ArrayList<Integer> x_qtr = new ArrayList<Integer>();
				for(int spare = 0; spare < x.size(); spare++){
					x_qtr.add(x.get(spare));
				}
				//FOR EACH DISCRETE ELEMENT USED
				ArrayList<Integer> broken_parts = new ArrayList<Integer>();
				ArrayList<Double> lead_times = new ArrayList<Double>();
				for(int d = 1; d < brkdwns.size(); d++){
					double mtbf = d_brkdwns.get(d).get(d_brkdwns.get(d).size()-1)[0];

					int time = q * 91; //IN DAYS
					double reliability = Math.exp(-time/mtbf);

					//DETERMINE IF THIS PART HAS FAILED THIS QUARTER
					if(Math.random() > reliability){
						//PART HAS FAILED

						///
						//DETERMINE IF NHA IS REPARABLE
						int level_of_des = d_brkdwns.get(d).size()-1;
						int l = level_of_des;
						boolean repair_poss = false;
						//CYCLE THROUGH EACH NHA, STARTING WITH IMMEDIATE AND WORKING UPWARDS
						while(l > 0){
							l--;
							int repair_possible = (int)Math.round(d_brkdwns.get(d).get(l)[2]);

							if(repair_possible > .5){
								//REPAIRS POSSIBLE!
								//ADD THE BROKEN PART OF THE REPARABLE UNIT TO THE BROKEN PARTS LIST
								broken_parts.add(brkdwns.get(d).get(l+1)[0]);
								lead_times.add(d_brkdwns.get(d).get(l+1)[3]);
								repair_poss = true;
								//System.out.println("repairing "+brkdwns.get(d).get(l+1)[0]);
								break;
							}
						}
						if(!repair_poss){
							//A REPAIR WAS NOT POSSIBLE, REQUIRE NEW LRU
							broken_parts.add(brkdwns.get(d).get(0)[0]);
							lead_times.add(d_brkdwns.get(d).get(l+1)[3]);
							//System.out.println("repairing "+brkdwns.get(d).get(0)[0]);
						}	
					}
				}
				//A LIST OF BROKEN PARTS THAT WILL BE REPLACED HAS NOW BEEN GENERATED
				//CHECK AGAINST SPARES TO DETERMINE IF THESE PARTS ARE AVAILABLE IN STOCK
				//IF NOT AVAILABLE IN THE SOLUTION IN THE REQUIRED QUANTITY, NEW ONE WILL BE ORDERED
				//PENALIZE BASED ON LEAD TIME
				//ASSUMUMPTION: THAT ONLY ONE PLANE AT MOST WILL BE GROUNDED DUE TO MISSING PART, OTHER PLANES WILL TAKE PARTS FROM THIS GROUNDED PLANE
				double longest_lead_time = 0;
				for(int bp = 0; bp < broken_parts.size(); bp++){
					int broken_part = broken_parts.get(bp);
					boolean in_spares = false;
					for(int sp = 0; sp < x_qtr.size(); sp++){
						int spare_part = x_qtr.get(sp);
						if(spare_part == broken_part){
							//MATCH WAS FOUND, REMOVE USED PART FROM SPARES LIST
							in_spares = true;
							x_qtr.set(sp, 0);
							//System.out.println("in spares" + broken_part + spare_part);
							break;
						}
					}
					if(!in_spares){
						//FIND LEAD TIME, KEEP THE TIME IF IT IS THE LONGEST FOUND SO FAR
						double lt = lead_times.get(bp);
						longest_lead_time = Math.max(longest_lead_time, lt);
					}
				}
				//WITH LONGEST LEAD TIME, APPLY PENALTY
				//System.out.println("longest lt = "+ longest_lead_time);
				cost = cost + longest_lead_time * lt_penalty;
				
				//PENALIZE FOR STOCK LEFT IN SPARES THAT WAS NOT UTILIZED
				double sum_of_costs = 0;
				for(int sp = 0; sp < x_qtr.size(); sp++){
					int spare_part = x_qtr.get(sp);

					if(spare_part < 1){
						//THEN THIS PART WAS USED, DON'T ADD IN
					}else{
						for(int dpi = 0; dpi < part_data.size(); dpi++){
							//System.out.println(part_data.get(dpi)[0]+"\t"+part_data.get(dpi)[2]);
							if(Math.abs(part_data.get(dpi)[0] - spare_part) < 1){
								//THEN THIS IS THE DATA FOR THIS PART
								//ADD TO ROLLING SUM
								sum_of_costs = sum_of_costs + part_data.get(dpi)[2];
								break;
							}
							
						}
					}
				}
				
				cost = cost + sum_of_costs * c_penalty;
				
				//System.out.println(cost);
			}
			obj_stat.print(cost+",");
			obj_stat.println();
			average_cost = average_cost + cost/N_Iterations;
		}
		
		
		return average_cost;
	}
	
	//LOADS DATA TO GLOBAL ARRAY
	public static void load_data() throws FileNotFoundException{
		//THIS WILL HOLD ALL DATA... MEGATRON
		ArrayList<ArrayList<ArrayList<Double>>> megatron = new ArrayList<ArrayList<ArrayList<Double>>>();
		
		///
		//LOAD LRU AND SAS BREAKDOWNS INTO SIMILAR ARRAYS
		loadBreakdowns();	
		System.out.println("basic stuff loaded\t"+lrus_brkdwns.size());
		///	
		
		//FIND STRUCTURE OF DATA, LRU, SAS, SAS, ... , SAS, DES
		get_detailed_parts_list();
		
		
		//NOW FILL IN MEGATRON, WITH ALL PART DATA FOR ALL PARTS FOUND ON FLEET
		//PN....... 
		//QTY...... PN AND QTY ALREADY EXIST IN ARRAY, CREATE NEW ARRAY OF SIMILAR STRUCTURE FOR THE REST OF THE PARTS
		//LT........... FOLLOWS HORIZONTAL STRUCTURE ALREADY IN PLACE IN DATA ARRAY, SWITCH TO DOUBLE[] AND PLACE ROWS FOR MISSING DATA
		//REPARABLE?
		//MTBF
		//COST
		
		//GET THE RAW DATA TO WORK WITH
		loadPartData();
		
		/*
		for(int i = 0; i < brkdwns.size(); i++){
			System.out.println();
			for(int j = 0; j < brkdwns.get(i).size(); j++){
				System.out.print(brkdwns.get(i).get(j)[0]+"\t"+brkdwns.get(i).get(j)[1]+"\t");
				
			}
		}
		*/
		
		//MATCH THE RAW DATA WITH BRKDWN
		for(int i = 0; i < brkdwns.size(); i++){
			ArrayList<double[]> temp2 = new ArrayList<double[]>();
			for(int j = 0; j < brkdwns.get(i).size(); j++){
				for(int k = 0; k < part_data.size(); k++){
					int part_in_data = (int)Math.round(part_data.get(k)[0]);
					if(brkdwns.get(i).get(j)[0] == part_in_data){
						//COPY DATA INTO COORESPONDING PLACE IN NEW DATASHEET
						double[] temp = {part_data.get(k)[1],part_data.get(k)[2],part_data.get(k)[3],part_data.get(k)[4]};
						temp2.add(temp);
						break;
					}

				}
			}
			d_brkdwns.add(temp2);
		}
		
		
		//TEST DATA TO MAKE SURE IT HAS LOADED PROPERLY
		data_used.println(brkdwns.size());
		for(int i = 0; i < brkdwns.size(); i++){
			data_used.println();
			for(int j = 0; j < brkdwns.get(i).size(); j++){
				data_used.print(brkdwns.get(i).get(j)[0]+","+brkdwns.get(i).get(j)[1] +","+ d_brkdwns.get(i).get(j)[0]+","+ d_brkdwns.get(i).get(j)[1]+","+ d_brkdwns.get(i).get(j)[2]+","+ d_brkdwns.get(i).get(j)[3]);
				
			}
		}
		data_used.close();
		
	}
	
	public static void get_detailed_parts_list() throws FileNotFoundException{
		
		for(int lru = 0; lru < lrus_brkdwns.size(); lru++){
			
			//System.out.println(lrus_brkdwns.get(lru).get(0)+"\t"+lrus_brkdwns.get(lru).get(1)+"\t"+lrus_brkdwns.get(lru).get(2)+"\t");
			
			if(lrus_brkdwns.get(lru).get(0) != null){
				//THIS IS AN LRU
				int[] lru_data = {lrus_brkdwns.get(lru).get(0),lrus_brkdwns.get(lru).get(2)};
				
				
				//CYCLE THROUGH DES AND SAS THAT MAKE UP THE PART
				int ipj = lru+1; //CYCLE UPWARDS UNTIL NEXT LRU IS FOUND, THEN TERMINATE THIS LRUS BREAKDOWN
				while(ipj < lrus_brkdwns.size() && lrus_brkdwns.get(ipj).get(1) != null){
					int qty = lrus_brkdwns.get(ipj).get(2);
					int part = lrus_brkdwns.get(ipj).get(1);
					
					ArrayList<int[]> lru_brkdwn = new ArrayList<int[]>();
					lru_brkdwn.add(lru_data);
					int[] this_data = {part,qty};
					lru_brkdwn.add(this_data);
					//IF PART IS DES, THEN ADD RIGHT AWAY, ELSE FIND SUBSEQUENT BREAKDOWN
					if(part > 300000){
						brkdwns.add(lru_brkdwn);
					}else{
						
						//THIS PART IS SAS, AND ANALYSIS BECOMES MORE DIFFICULT
						//FIND SUBSEQUENT BREAKDOWN, AND LOAD EACH ROW TO brkdwns IF NOT A SAS, IF SAS, THEN REPEAT
						ArrayList<int[]> sas_brkdwn = brkdwn(part);
						
						for(int k = 0; k < sas_brkdwn.size(); k++){
							int[] this_data2 = {sas_brkdwn.get(k)[0],sas_brkdwn.get(k)[1]};
							if(sas_brkdwn.get(k)[0] > 300000){
								//THIS ELEMENT IS A DES, ADD TO OVERALL BREAKDOWN
								ArrayList<int[]> runing_brkdwn = new ArrayList<int[]>();
								for(int c = 0; c < lru_brkdwn.size(); c++){
									runing_brkdwn.add(lru_brkdwn.get(c));
								}
								runing_brkdwn.add(this_data2);
								//ROLL UP
								brkdwns.add(runing_brkdwn);							
							}else{
								//ANOTHER SAS EXISTS
								ArrayList<int[]> running_brkdwn = new ArrayList<int[]>();
								for(int c = 0; c < lru_brkdwn.size(); c++){
									running_brkdwn.add(lru_brkdwn.get(c));
								}
								running_brkdwn.add(this_data2);
								parts_looper(brkdwns, this_data2, running_brkdwn);
								//SIMILAR TO ABOVE
								
								//System.out.println("SAS!!!");
							}
						}
					}
					
					ipj++;
				}
				
			}
		}
	}
	
	//LOAD ALL OF THE PART DATA
	public static void loadPartData() throws FileNotFoundException{
		Scanner scan = new Scanner(new File(directory+part_data_file_name));
		while(scan.hasNextLine()){
			String str = scan.nextLine();
			str = str.replaceAll(",","\t");
			Scanner s1 = new Scanner(str);
			int part = s1.nextInt();
			double mbtf = Math.max(0, s1.nextDouble());
			double cost = Math.max(0, s1.nextDouble());
			double reparable = Math.max(0, s1.nextDouble());
			double leadtime = Math.max(0,s1.nextDouble());
			double[] temp = {part,mbtf,cost,reparable,leadtime};
			part_data.add(temp);
			
		}
		
	}
	
	//GET BREAKDOWN OF SAS PART, RETURN LIST OF PARTNUMBERS AND QUANTITY USED FOR THIS SAS
	public static ArrayList<int[]> brkdwn(int part) throws FileNotFoundException{
		
		//STORE [PN][QTY OF] FOR EACH PART OF THE BREAKDOWN
		ArrayList<int[]> brkdwn = new ArrayList<int[]>();
		
		
		for(int i = 0; i < sas_brkdwns.size(); i++){
			int brk_part = -1;
			if(sas_brkdwns.get(i).get(0) == null){
				//DO NOT MATCH AGAINST SUB-ASSEMBLIES IN THE SUB ASSEMBLY BREAKDOWN
				//brk_part = sas_brkdwns.get(i).get(1);
			}else{
				brk_part = sas_brkdwns.get(i).get(0);
			}
			//IS THIS THE RIGHT PART?
			if(brk_part == part){
				//MATCHED! GET BREAKDOWN
				int ipj = i+1; //STARTS FROM i, MOVES UNTIL ANOTHER NHA IS FOUND
				while(sas_brkdwns.get(ipj).get(1) != null){
					int[] temp = new int[2];
					temp[0] = sas_brkdwns.get(ipj).get(1);
					temp[1] = sas_brkdwns.get(ipj).get(2);
					brkdwn.add(temp);
					ipj++;
				}
			}
		}
				
		return brkdwn;
	}
	
	public static void loadBreakdowns() throws FileNotFoundException{
		///
		//LOAD LRU AND SAS BREAKDOWNS INTO SIMILAR ARRAYS
		//FOR THE LRU DATA
		Scanner lru = new Scanner(new File(directory+lru_file_name));
		while(lru.hasNextLine()){
			String line = lru.nextLine();
			//FIND PART AND QUANTITY FROM THIS LINE OF DATA
			Scanner ls = new Scanner(line.replaceAll(",", "\t"));
			int part = ls.nextInt();
			int qty = ls.nextInt();
			//ADD DATA TO TEMPORARY ARRAY OF THREE, DEPENDING ON NHA OR ELEMENT STATUS
			ArrayList<Integer> temp = new ArrayList<Integer>();
			//NHA OR ELEMENT?
			if(line.charAt(0)==','){
				//ELEMENT
				temp.add(null); //NOT NHA
				temp.add(part);
				temp.add(qty);
			}else{
				//NHA
				temp.add(part);
				temp.add(null); //NOT ELELEMENT
				temp.add(qty);
			}
			//RECORD TEMPORARY TO OVERALL STORAGE
			lrus_brkdwns.add(temp);
		}
		//FOR THE SAS DATA
		Scanner sas = new Scanner(new File(directory+sas_file_name));
		while(sas.hasNextLine()){
			String line = sas.nextLine();
			//FIND PART AND QUANTITY FROM THIS LINE OF DATA
			Scanner ls = new Scanner(line.replaceAll(",", "\t"));
			int part = ls.nextInt();
			int qty = ls.nextInt();
			//ADD DATA TO TEMPORARY ARRAY OF THREE, DEPENDING ON NHA OR ELEMENT STATUS
			ArrayList<Integer> temp = new ArrayList<Integer>();
			//NHA OR ELEMENT?
			if(line.charAt(0)==','){
				//ELEMENT
				temp.add(null); //NOT NHA
				temp.add(part);
				temp.add(qty);
			}else{
				//NHA
				temp.add(part);
				temp.add(null); //NOT ELELEMENT
				temp.add(qty);
			}
			//RECORD TEMPORARY TO OVERALL STORAGE
			sas_brkdwns.add(temp);
		}		
		///
	}
	
	//TAKES SAS PART AND LIST OF PARTS PRIOR, RETURNS ARRAY OF PARTS AND QTYS
	public static ArrayList<ArrayList<int[]>> parts_looper(ArrayList<ArrayList<int[]>> brkdwns, int[] this_sas_data, ArrayList<int[]> running_brkdwns) throws FileNotFoundException{

		int part = this_sas_data[0];
		ArrayList<int[]> sas_brkdwn = brkdwn(part);
		
		for(int k = 0; k < sas_brkdwn.size(); k++){
			int[] this_data2 = {sas_brkdwn.get(k)[0],sas_brkdwn.get(k)[1]};
			if(sas_brkdwn.get(k)[0] > 300000){
				//THIS ELEMENT IS A DES, ADD TO OVERALL BREAKDOWN
				ArrayList<int[]> runing_brkdwn2 = new ArrayList<int[]>();
				for(int c = 0; c < running_brkdwns.size(); c++){
					runing_brkdwn2.add(running_brkdwns.get(c));
				}
				runing_brkdwn2.add(this_data2);
				//ROLL UP
				brkdwns.add(runing_brkdwn2);							
			}else{
				//ANOTHER SAS EXISTS
				ArrayList<int[]> runing_brkdwn2 = new ArrayList<int[]>();
				for(int c = 0; c < running_brkdwns.size(); c++){
					runing_brkdwn2.add(running_brkdwns.get(c));
				}
				runing_brkdwn2.add(this_data2);
				parts_looper(brkdwns, this_data2, runing_brkdwn2);
				//SIMILAR TO ABOVE
				
				//System.out.println("SAS!!!");
			}
		}
		return brkdwns;
	}
}
