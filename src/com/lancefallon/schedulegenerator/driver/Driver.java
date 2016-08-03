package com.lancefallon.schedulegenerator.driver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lancefallon.schedulegenerator.model.Division;
import com.lancefallon.schedulegenerator.model.Game;
import com.lancefallon.schedulegenerator.model.Team;
import com.lancefallon.schedulegenerator.model.Week;
import com.lancefallon.schedulegenerator.service.DataStore;

public class Driver {

	static DataStore dataStore;
	static int maxWeeks = 0;
	static List<Week> weeks;
	static List<Team> teams = new ArrayList<>();
	static List<Team> noMatches = new ArrayList<Team>();
	static List<Game> possibleMatchups = new ArrayList<>();
	
	public static void main(String[] args) throws IOException {
		for(int i = 0; i < 100; i++){
			boolean finished = false;
			do{
				 finished = generateSchedule();
			} while(!finished);
			
			organizeSchedule();
			
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(new File("/Users/lfallon/Desktop/nfl_schedule_" + new Date().getTime() + ".json"), weeks);
			System.out.println("finished " + (i+1) + " schedules");
		}
	}
	
	private static void organizeSchedule(){
		for(Team team : teams){
			
			//get all games for team
			List<Game> pool = weeks.stream()
					.flatMap(w -> w.getGames().stream())
					.filter((g -> g.getHomeTeam().equals(team) || g.getAwayTeam().equals(team)))
					.collect(Collectors.toList());
			
			//out-of-conference games
			List<Game> outOfConference = pool.stream().filter(g -> {
				return (g.getHomeTeam().equals(team) && !g.getAwayTeam().getDivision().getConference().equals(team.getDivision().getConference())) ||
					(g.getAwayTeam().equals(team) && !g.getHomeTeam().getDivision().getConference().equals(team.getDivision().getConference()));
			}).collect(Collectors.toList());
			evenHomeAway(team, outOfConference);
			
			//in-conference-divisional-matchup
			List<Game> inConferenceMatchup = pool.stream().filter(g -> {
				
				if(g.getHomeTeam().equals(team)){
					for(Entry<Division, Division> entry : dataStore.getInConferenceTable().entrySet()) {
					    Division division1 = entry.getKey();
					    Division division2 = entry.getValue();
					    
					    //can play if in division matchup table
					    if((g.getHomeTeam().getDivision().equals(division1) && g.getAwayTeam().getDivision().equals(division2)) ||
					    		(g.getHomeTeam().getDivision().equals(division2) && g.getAwayTeam().getDivision().equals(division1))){
					    	return true;
					    }
					}
				} else{
					for(Entry<Division, Division> entry : dataStore.getInConferenceTable().entrySet()) {
					    Division division1 = entry.getKey();
					    Division division2 = entry.getValue();
					    
					    //can play if in division matchup table
					    if((g.getAwayTeam().getDivision().equals(division1) && g.getHomeTeam().getDivision().equals(division2)) ||
					    		(g.getAwayTeam().getDivision().equals(division2) && g.getHomeTeam().getDivision().equals(division1))){
					    	return true;
					    }
					}
				}
			
				return false;
				
			}).collect(Collectors.toList());
			evenHomeAway(team, inConferenceMatchup);
			
			//in-conference-divisional-matchup by rank
			List<Game> inConferenceMatchupByRank = pool.stream().filter(g -> {
				
				Division divisionMatchup1 = null;
				Division divisionMatchup2 = null;
				
				for(Entry<Division, Division> entry : dataStore.getInConferenceTable().entrySet()) {
				    Division division1 = entry.getKey();
				    Division division2 = entry.getValue();
				    
				    //can play if in division matchup table
				    if((team.getDivision().equals(division1)) || 
				    		team.equals(division2)){
				    	divisionMatchup1 = entry.getKey();
				    	divisionMatchup2 = entry.getValue();
				    }
				}
				
				if(g.getHomeTeam().getDivision().equals(g.getAwayTeam().getDivision())){
					return false;
				}
				
				if(g.getHomeTeam().equals(team) && g.getAwayTeam().getDivision().getConference().equals(team.getDivision().getConference())){

					if(!(g.getAwayTeam().getDivision().equals(divisionMatchup1) || g.getAwayTeam().getDivision().equals(divisionMatchup2))){
						return true;
					}
					
				} else if(g.getAwayTeam().equals(team) && g.getHomeTeam().getDivision().getConference().equals(team.getDivision().getConference())){
					if(!(g.getHomeTeam().getDivision().equals(divisionMatchup1) || g.getHomeTeam().getDivision().equals(divisionMatchup2))){
						return true;
					}
				}
			
				return false;
				
			}).collect(Collectors.toList());
			evenHomeAway(team, inConferenceMatchupByRank);
		}
	}
	
	private static void evenHomeAway(Team team, List<Game> games){
		List<Game> homeGames = games.stream().filter(g->g.getHomeTeam().equals(team)).collect(Collectors.toList());
		List<Game> awayGames = games.stream().filter(g->g.getAwayTeam().equals(team)).collect(Collectors.toList());
		if(homeGames.size() > awayGames.size()){
			int diff = homeGames.size() - awayGames.size();
			for(int i = diff/2 - 1; i >= 0; i--){
				Team tempHome = homeGames.get(i).getHomeTeam();
				Team tempAway = homeGames.get(i).getAwayTeam();
				homeGames.get(i).setHomeTeam(tempAway);
				homeGames.get(i).setAwayTeam(tempHome);
			}
		}
		else if(homeGames.size() < awayGames.size()){
			int diff = awayGames.size() - homeGames.size();
			for(int i = diff/2 - 1; i >= 0; i--){
				Team tempHome = awayGames.get(i).getHomeTeam();
				Team tempAway = awayGames.get(i).getAwayTeam();
				awayGames.get(i).setHomeTeam(tempAway);
				awayGames.get(i).setAwayTeam(tempHome);
			}

		}
	}
	
	private static boolean generateSchedule(){
		dataStore = DataStore.getInstance();
		teams = dataStore.getTeams();
			
		loadPossibleMatchups();
		
		weeks = new ArrayList<Week>();
		
		//loop through 16 weeks
		for(int i = 0; i < 16; i++){
			boolean weekSatisfied = false;
			int attempt = 0;
			while(!weekSatisfied && attempt < 1000){
				attempt++;
				Week week = new Week();
				int counter = 0;
				randomizeRemainingMatchups();
				
				//for each available matchup table (by team)
				while(week.getGames().size() <= 16 && counter < possibleMatchups.size()){
					counter = 0;
					//loop through their available games
					for(Game game : possibleMatchups){
						counter++;
						if(!isPlayingInWeek(week, game.getHomeTeam()) && !isPlayingInWeek(week, game.getAwayTeam())){
							week.getGames().add(game);
							possibleMatchups.remove(game);
							break;
						}
					}
				}
				if(week.getGames().size() == 16){
					weekSatisfied = true;
					weeks.add(week);
					if(weeks.size() > maxWeeks){
						 maxWeeks = weeks.size();
						 System.out.println("max weeks: " + maxWeeks);
					}
				} else{
					for(int j = 0; j < week.getGames().size(); j++){
						possibleMatchups.add(week.getGames().get(j));
					}
				}
			}
		}
		
		return weeks.size() == 16;
	}
	
	private static void randomizeRemainingMatchups() {
		for(Game game : possibleMatchups){
			game.setRandomSeed(new Random().nextLong());
		}
		possibleMatchups.sort((a,b)->a.getRandomSeed() > b.getRandomSeed() ? 1 : a.getRandomSeed() < b.getRandomSeed() ? -1 : 0);
	}

	private static void loadPossibleMatchups(){
		for(int i = 0; i < teams.size(); i++){
			Team team = teams.get(i);
			for(int j = 0; j < teams.size(); j++){
				if(teams.get(j).equals(team))
					continue;
				
				if(canFaceOpponent(team, teams.get(j))){
					Game game = new Game();
					game.setHomeTeam(team);
					game.setAwayTeam(teams.get(j));
					game.setRandomSeed(new Random().nextLong());
					possibleMatchups.add(game);
				}
			}
		}
		
		for(int i = 0; i < possibleMatchups.size(); i++){
			for(int j = 0; j < possibleMatchups.size(); j++){
				if(possibleMatchups.get(i).getRandomSeed().equals(possibleMatchups.get(j).getRandomSeed()))
					continue;
				
				if(possibleMatchups.get(i).equals(possibleMatchups.get(j))){
					possibleMatchups.remove(possibleMatchups.get(j));
				}
			}
		}
		
		possibleMatchups.sort((a,b)->a.getRandomSeed() > b.getRandomSeed() ? 1 : a.getRandomSeed() < b.getRandomSeed() ? -1 : 0);
	}
	
	private static boolean canFaceOpponent(Team team, Team opponent) {		
		//if outside of conference
		if(checkConferenceTable(team, opponent)){
			return true;
		}
		
		//if in-conference
		return checkInConference(team, opponent);
	}
	
	public static Boolean checkInConference(Team team, Team opponent){
		if(!team.getDivision().equals(opponent.getDivision())){
			
			//check if in division matchup table
			for(Entry<Division, Division> entry : dataStore.getInConferenceTable().entrySet()) {
			    Division division1 = entry.getKey();
			    Division division2 = entry.getValue();
			    
			    //can play if in division matchup table
			    if((team.getDivision().equals(division1) && opponent.getDivision().equals(division2)) ||
			    		(team.getDivision().equals(division2) && opponent.getDivision().equals(division1))){
			    	return true;
			    }
			}
			
	    	//check if outside division & outside division matchup table, but play based on prev season final standings
			return conferenceStandingsMatch(team, opponent);
		}
		
		//if in own division, they can play
		return true;
	}
	
	/**
	 * return if two teams can play based on the conference matchup table
	 * @param team
	 * @param opponent
	 * @return
	 */
	public static Boolean checkConferenceTable(Team team, Team opponent){
		if(!team.getDivision().getConference().equals(opponent.getDivision().getConference())){
			for(Entry<Division, Division> entry : dataStore.getOutOfConferenceTable().entrySet()) {
			    Division division1 = entry.getKey();
			    Division division2 = entry.getValue();
			    
			    //can play if in conference matchup table
			    if((team.getDivision().equals(division1) && opponent.getDivision().equals(division2)) ||
			    		(team.getDivision().equals(division2) && opponent.getDivision().equals(division1))){
			    	return true;
			    }
			}
		}
		return false;
	}

	/**
	 * return if a team is playing in the specified week
	 * @param week
	 * @param team
	 * @return
	 */
	public static Boolean isPlayingInWeek(Week week, Team team){
		List<Game> games = week.getGames();
		for(int i = 0; i < games.size(); i++){
			if(games.get(i).getAwayTeam().equals(team) || games.get(i).getHomeTeam().equals(team)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * return list of games where two given teams have played
	 * @param weeks
	 * @param team
	 * @param opponent
	 * @return
	 */
	public static List<Game> hasPlayedOpponent(List<Week> weeks, Team team, Team opponent){
		List<Game> gamesFacingOpponent = new ArrayList<Game>();
		for (int i = 0; i < weeks.size(); i++) {
			List<Game> games = weeks.get(i).getGames();
			for(int j = 0; j < games.size(); j++){
				if(games.get(j).getAwayTeam().equals(team) && games.get(j).getHomeTeam().equals(opponent) ||
					games.get(j).getAwayTeam().equals(opponent) && games.get(j).getHomeTeam().equals(team)){
					gamesFacingOpponent.add(games.get(j));
				}
			}
		}
		return gamesFacingOpponent;
	}
	
	/**
	 * return if in same conference and finished in same position within own division prior season
	 * @param team
	 * @param opponent
	 * @return
	 */
	public static Boolean conferenceStandingsMatch(Team team, Team opponent){
		return team.getDivision().getConference().equals(opponent.getDivision().getConference()) && team.getDivisionRank().equals(opponent.getDivisionRank());
	}

}
