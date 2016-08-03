package com.lancefallon.schedulegenerator.driver;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lancefallon.schedulegenerator.service.DataStore;
import com.lancefallon.schedulegenerator.service.ScheduleGenerationService;

public class Driver {

	static final int LIMIT = 5;
	private static ScheduleGenerationService scheduleGenerationService;
	
	/**
	 * entry point
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		
		//initialize datastore (fake / hard-coded right now)
		scheduleGenerationService = new ScheduleGenerationService(DataStore.getInstance());
		
		//get number of schedules to create (either from command line arg or constant)
		int limit = 0;
		try{
			limit = Integer.parseInt(args[0]);
		} catch(Exception e){
			limit = LIMIT;
		}
		
		start(limit);
	}
	
	/**
	 * start schedule creation
	 * @param limit
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private static void start(int limit) throws JsonGenerationException, JsonMappingException, IOException{
		 
		
		//start schedule creation loop
		for(int i = 0; i < limit; i++){
			
			//keep trying to generate a schedule until each team has 16 games
			boolean finished = false;
			do{
				 finished = scheduleGenerationService.generateSchedule();
			} while(!finished);
			
			//organize the home / away games. perform passes until each team has 8 home and away games
			int flips = 0;
			do{
				//return how many "flips" (home / away switches) were performed. if none, then all teams have right # of home / away games and can break out of loop
				flips = scheduleGenerationService.organizeHomeAndAway();
				System.out.println(flips);
			}
			while(flips > 0);
			
			//garbage - don't need to spit anything out, but doing it to anylyze with javascript in browser
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(new File("/Users/lfallon/Desktop/nfl_schedule_" + new Date().getTime() + ".json"), scheduleGenerationService.getWeeks());
			System.out.println("finished " + (i+1) + " of " + limit + " schedules");
		}
	}
	


}
