package Spares_Algorithm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

/**
 * THIS WILL GENERATE THE PART DATA, TO BE STORED IN CSV
 * 
 * **/

public class Data_Generator {

	final static int n_lrus = 10;	//NUMBER OF LRUS
	final static int n_sas = 10*n_lrus;	//NUMBER OF SUB ASSEMBLIES
	final static int n_des = 100*n_lrus;	//NUMBER OF DISCRETE ELEMENTS
	final static int lru_des_max = 100; //MAXIMUM NUMBER OF DES ASSIGNED DIRECTLY TO LRU
	final static int lru_des_min = 1;	//MINIMUM NUMBER OF DES ASSIGNED DIRECTLY TO LRU
	final static int lru_sas_max = 15; //MAXIMUM NUMBER OF SAS ASSIGNED DIRECTLY TO LRU
	final static int lru_sas_min = 0;	//MINIMUM NUMBER OF SAS ASSIGNED DIRECTLY TO LRU
	final static int sas_des_max = 50; //MAXIMUM NUMBER OF DES ASSIGNED DIRECTLY TO SAS
	final static int sas_des_min = 1;	//MINIMUM NUMBER OF DES ASSIGNED DIRECTLY TO SAS	
	final static int sas_sas_max = 4; //MAXIMUM NUMBER OF SAS ASSIGNED DIRECTLY TO SAS
	final static int sas_sas_min = 1;	//MINIMUM NUMBER OF SAS ASSIGNED DIRECTLY TO SAS
	final static double sMBTF = 300; //STDEV FOR RANDOM MBTF VALUES
	final static double mMBTF = 1825; //MEAN STDEV FOR RANDOM MBTF VALUES
	final static double sCost = 25; //STANDARD DEVIATION FOR RANDOM COST VALUES
	final static double mCost = 100; //MEAN COST FOR RANDOM VALUES
	final static double sPR = .12; //STANDARD DEVIATION FOR RANDOM REPAIR POSSIBLIE VALUES
	final static double mPR = .6; //MEAN PROBABILITY OF REPAIR
	final static double sLT = 8; //STANDARD DEVIATION FOR LEAD TIME
	final static double mLT = 15; //MEAN LEAD TIME
	final static double prob_of_mult_sas = .1; //PROBABILITY THAT THERE WILL BE A SAS ASSIGNED TO A SAS. LOW IS MORE REALISTIC
	
	/*
	 * 			des_data[i][0] = r.nextGaussian()*sMTBF + mMBTF;
			des_data[i][1] = r.nextGaussian()*sCost + mCost;
			des_data[i][2] = r.nextGuassian()*sPR + mPR;
			des_data[i][3] = r.nextGuassian()*sLT + mLT;
	 * */
	
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		
		rg();
		
		int fleet_size = 35;
		
		String[] LRU_base = {
				"Coffee Maker",
				"Water Boiler",
				"Meal Cart",
				"Window Shade",
				"Monument",
				"Lavatorie",
				"Toilet",
				"Faucet",
				"Dispenser",
				"Light",
				"PSU",
				"Attendent Seat",
				"ELT",
				"Megaphone",
				"Slide/Raft"
		};
		
		int[] LRU_models = {2,1,2,5,3,3,1,1,1,30,2,4,1,1,4};
		
		double[] LRU_MRP = {.6,.6,.9,.3,.999,.999,.6,.2,.1,.5,.7,.95,.2,.1,.98};
		
		double[] LRU_cost = {19600,15960,26600,7000,140000,182000,16380,9800,9800,8680,42840,36400,49000,1050,350000};
		
		int[][] SC = {
				{3,60,10},
				{2,50,7},
				{0,80,15},
				{2,5,5},
				{3,5,45},
				{5,100,30},
				{1,3,10},
				{1,2,5},
				{1,2,5},
				{1,60,5},
				{3,40,28},
				{0,100,30},
				{3,30,5},
				{1,10,5},
				{0,5,5}
		};
		
		

	}
	
	public static void rg() throws FileNotFoundException{
		
		///
		//GENERATE LISTS OF POSSIBLE PARTS
		
		int[] lrus = new int[n_lrus];
		int[] sas = new int[n_sas];
		int[] des = new int[n_des];
		

		
		
		
		
		//LRUs (TOP LEVEL ASSEMBLIES)
		for(int j = 0; j < lrus.length; j++){
			lrus[j] = 100000+j;
		}	
		//SUB ASSEMBLY GENERATION
		for(int j = 0; j < sas.length; j++){
			sas[j] = 200000+j;
		}
		//DISCRETE ELLEMENTS GENERATION
		for(int j = 0; j < des.length; j++){
			des[j]  = 300000 + j;
		}
		
		//LIST COMPLETED
		///
		
		///
		//ASSIGN DES AND SAS TO LRUS
		int[][] lrus_brkdwn = new int[n_lrus][];
		for(int j = 0; j < n_lrus; j++){
			//DETERMINE NUMBER OF DES TO ADD
			int n_des_lru = (int)Math.round(Math.max(lru_des_min, Math.random()*lru_des_max));
			//DETERMINE NUMBER OF SAS TO ADD
			int n_sas_lru = (int)Math.round(Math.max(lru_sas_min, Math.random()*lru_sas_max));
			//GENERATE PARTS TO ADD
			int[] subs = new int[n_des_lru+n_sas_lru];
			for(int i = 0; i < n_des_lru; i++){
				subs[i] = des[(int)Math.round(Math.random()*(des.length-1))];
			}
			for(int i = n_des_lru; i < subs.length; i++){
				subs[i] = sas[(int)Math.round(Math.random()*(sas.length-1))];
			}
			lrus_brkdwn[j] = subs;
		}
		
		//ASSIGN DES and sas TO SAS
		int[][] sas_brkdwn = new int[n_sas][];
		for(int j = 0; j < n_sas; j++){
			//DETERMINE NUMBER OF DES TO ADD
			int n_des_sas = (int)Math.round(Math.max(sas_des_min, Math.random()*sas_des_max));
			//DETERMINE NUMBER OF SAS TO ADD
			int n_sas_sas = 0;
			if(Math.random() < prob_of_mult_sas){
				n_sas_sas = (int)Math.round(Math.max(sas_sas_min, Math.random()*sas_sas_max));
			}else{
				n_sas_sas = 0;
			}
			//GENERATE PARTS TO ADD
			int[] subs = new int[n_des_sas+n_sas_sas];
			for(int i = 0; i < n_des_sas; i++){
				subs[i] = des[(int)Math.round(Math.random()*(des.length-1))];
			}
			
			for(int i = n_des_sas; i < subs.length; i++){
				//CANNOT ASSIGN SELF TO SELF...
				boolean match = true;
				while(match){
					subs[i] = sas[(int)Math.round(Math.random()*(sas.length-1))];
					if(subs[i] == sas[j]){
						match = true;
					}else{
						match = false;
					}
				}
			}
			sas_brkdwn[j] = subs;
		}
		
		///
		//ASSIGN COST AND MBTUF INFORMATION FOR EACH PART
		Random r = new Random();
		//DES
		double[][] des_data = new double[n_des][4]; //MTBF/COST/PROB TO REPAIR/LEAD TIME
		for(int i = 0; i < n_des; i++){
			des_data[i][0] = r.nextGaussian()*sMBTF + mMBTF;
			des_data[i][1] = r.nextGaussian()*sCost + mCost;
			des_data[i][2] = 0; //DES NOT REPARABLE //r.nextGaussian()*sPR + mPR;
			des_data[i][3] = r.nextGaussian()*sLT + mLT;
			//System.out.println(des[i]+"\t"+des_data[i][0]+"\t"+des_data[i][1]+"\t"+des_data[i][2]+"\t"+des_data[i][3]);
		}
		
		//System.out.println(cost_calc(lrus[1], des_data, sas_brkdwn, lrus_brkdwn, des, sas, lrus));
		//LRUS
		double[][] lrus_data = new double[n_lrus][4];
		for(int i = 0; i < lrus.length; i++){
			System.out.println(i);
			int lru = lrus[i];
			lrus_data[i][1] = cost_calc(lru, des_data, sas_brkdwn, lrus_brkdwn, des, sas, lrus);
			if(Math.random() < r.nextGaussian()*sPR + mPR){
				lrus_data[i][2] = 1;
			}else{
				lrus_data[i][2] = 0;
			}
			lrus_data[i][3] = r.nextGaussian()*sLT*1.5 + mLT*2;
		}
		//SAS
		double[][] sas_data = new double[n_sas][4];
		for(int i = 0; i < sas.length; i++){
			System.out.println(i);
			int sa = sas[i];
			sas_data[i][1] = cost_calc(sa, des_data, sas_brkdwn, lrus_brkdwn, des, sas, lrus);
			if(Math.random() < r.nextGaussian()*sPR + mPR){
				sas_data[i][2] = 1;
			}else{
				sas_data[i][2] = 0;
			}
			sas_data[i][3] = r.nextGaussian()*sLT*1.5 + mLT*2;
		}
		
		PrintWriter pw = new PrintWriter(new File("C:\\data analysis\\project\\DPL_LRU.csv"));
		for(int i = 0; i < lrus.length; i++){
			pw.println(""+lrus[i] + ",," + (int)Math.round(Math.random()*3 + 1));
			for(int j = 0; j < lrus_brkdwn[i].length; j++){
				pw.println(","+lrus_brkdwn[i][j]+","+(int)Math.round(Math.random()*5 + 1));
			}
		}
		pw.close();
		PrintWriter pw2 = new PrintWriter(new File("C:\\data analysis\\project\\DPL_SA.csv"));
		for(int i = 0; i < sas.length; i++){
			pw2.println(""+sas[i] +",,"+(int)Math.round(Math.random()*2 + 1));
			for(int j = 0; j < sas_brkdwn[i].length; j++){
				pw2.println(","+sas_brkdwn[i][j]+","+(int)Math.round(Math.random()*6 + 1));
			}
		}
		pw2.close();
		
		PrintWriter pw3 = new PrintWriter(new File("C:\\data analysis\\project\\part_data.csv"));
		for(int i = 0; i < lrus.length; i++){
			pw3.println(lrus[i]+","+lrus_data[i][0]+","+lrus_data[i][1]+","+lrus_data[i][2]+","+lrus_data[i][3]);
		}
		for(int i = 0; i < sas.length; i++){
			pw3.println(sas[i]+","+sas_data[i][0]+","+sas_data[i][1]+","+sas_data[i][2]+","+sas_data[i][3]);
		}
		for(int i = 0; i < des.length; i++){
			pw3.println(des[i]+","+des_data[i][0]+","+des_data[i][1]+","+des_data[i][2]+","+des_data[i][3]);
		}
		pw3.close();
	}

	//COST CALCULATOR FOR SAS AND LRU
	public static double cost_calc(int part, double[][] des_data, int[][] sas_brkdwn, int[][] lru_brkdwn, int[] des, int[] sas, int[] lru){
		double cost = 0;
		ArrayList<Integer> des_parts_used = new ArrayList<Integer>();
		ArrayList<Integer> sas_parts_used = new ArrayList<Integer>();
		int[] brk;
		
		if(part < 200000){
			//LRU
			int lru_index = 0;
			for(int i = 0; i < lru.length; i++){
				if(lru[i] == part){
					//MATCH
					lru_index = i;
					break;
				}
			}
			//FILL OUT ALL DES AND SAS UNITS USED
			brk = lru_brkdwn[lru_index];
		}else{
			//SA
			int sas_index = 0;
			for(int i = 0; i < sas.length; i++){
				if(sas[i] == part){
					//MATCH
					sas_index = i;
					break;
				}
			}
			//FILL OUT ALL DES AND SAS UNITS USED
			brk = sas_brkdwn[sas_index];			
		}
		
		//GET DOWN TO THE DISCRETE ELEMENT LEVEL FOR ALL PARTS
		for(int i = 0; i < brk.length; i++){
			if(brk[i] < 300000){
				//SAS
				sas_parts_used.add(brk[i]);
			}else{
				//DES
				des_parts_used.add(brk[i]);
			}
		}
		
		
		//DO NOT STOP UNTIL ONLY DES REMAIN
		
		boolean sas_found = true;
		while (sas_parts_used.size() >= 1){
			//System.out.println(sas_parts_used.size());
			/*
			sas_found = false;
			//FIND SAS INDEX FOR SAS PART NUMBER
			//FIND DES OR SAS BREAKDOWN FOR SAS PART NUMBER INDEX
			//ADD DES AND SAS TO APPLICABLE LISTS
			//REMOVE THIS SAS FROM SAS LIST
			int sas_part = sas_parts_used.get(0);
			int sas_index = 0;
			for(int i = 0; i < sas.length; i++){
				if(sas[i] == sas_part){
					//MATCH
					sas_index = i;
					break;
				}
			}
			//FILL OUT ALL DES AND SAS UNITS USED
			brk = sas_brkdwn[sas_index];			
			//GET DOWN TO THE DISCRETE ELEMENT LEVEL FOR ALL PARTS
			for(int i = 0; i < brk.length; i++){
				if(brk[i] < 300000){
					//SAS
					sas_parts_used.add(brk[i]);
					sas_found = true;
				}else{
					//DES
					des_parts_used.add(brk[i]);
				}
			}
			*/
			int sas_part = sas_parts_used.get(0);
			cost = cost + cost_calc(sas_part, des_data, sas_brkdwn, lru_brkdwn, des, sas, lru);
			sas_parts_used.remove(0);
		}
		
		///
		//WITH LIST OF DES, LOOK UP PRICE FOR EACH ELEMENT USED ON PART, ADD TO SUM
		for(int i = 0; i < des_parts_used.size(); i++){
			//FIRST, FIND INDEX OF PART NUMBER
			int des_part = des_parts_used.get(i);
			int des_index = -1;
			for(int j = 0; j < des.length; j++){
				if(des[j] == des_part){
					des_index = j;
					break;
				}
			}
			//SECOND, ADD COST OF PART TO GROWING SUM
			cost = cost + des_data[des_index][1];
		}
		
		//MARKUP STANDARD 40%
		
		cost = cost*1.4;
		
		return cost;
	}
	
	
}
