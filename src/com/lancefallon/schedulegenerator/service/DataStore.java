package com.lancefallon.schedulegenerator.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.lancefallon.schedulegenerator.model.Conference;
import com.lancefallon.schedulegenerator.model.Division;
import com.lancefallon.schedulegenerator.model.Team;

public class DataStore {

	//accessible via getters / setters
	private List<Division> divisions;
	private List<Conference> conferences;
	private List<Team> teams;
	
	Map<Division, Division> inConferenceTable;
	Map<Division, Division> outOfConferenceTable;
	
	//not accessible outside of class
	private Conference afc;
	private Conference nfc;
	
	private Division afcEast;
	private Division afcNorth;
	private Division afcSouth;
	private Division afcWest;
	private Division nfcEast;
	private Division nfcNorth;
	private Division nfcSouth;
	private Division nfcWest;
	
	private static DataStore instance = null;
	
	private DataStore(){ }
	
	public static DataStore getInstance(){
		if(instance == null){
			instance = new DataStore();
			instance.init();
		}
		return instance;
	}
	
	private void init(){
		this.afc = new Conference(1,"AFC");
		this.nfc = new Conference(2,"NFC");
		
		this.afcEast = new Division(1,"AFC East", afc);
		this.afcNorth = new Division(2,"AFC North", afc);
		this.afcSouth = new Division(3,"AFC South", afc);
		this.afcWest = new Division(4,"AFC West", afc);
		this.nfcEast = new Division(5,"NFC East", nfc);
		this.nfcNorth = new Division(6,"NFC North", nfc);
		this.nfcSouth = new Division(7,"NFC South", nfc);
		this.nfcWest = new Division(8,"NFC West", nfc);

		divisions = Arrays.asList(afcEast, afcNorth, afcSouth, afcWest, nfcEast, nfcNorth, nfcSouth, nfcWest);
		conferences = Arrays.asList(afc,nfc);
		
		//setup the matchup tables
		this.inConferenceTable = new HashMap<Division, Division>();
		this.outOfConferenceTable = new HashMap<Division, Division>();
		inConferenceTable.put(afcEast, afcNorth);
		inConferenceTable.put(afcSouth, afcWest);
		inConferenceTable.put(nfcEast, nfcNorth);
		inConferenceTable.put(nfcSouth, nfcWest);
		
		outOfConferenceTable.put(afcEast, nfcEast);
		outOfConferenceTable.put(afcNorth, nfcNorth);
		outOfConferenceTable.put(afcSouth, nfcSouth);
		outOfConferenceTable.put(afcWest, nfcWest);
		
		generateTeams();
	}
	
	public void generateTeams(){
		Random r = new Random();
		Team arizona_cardinals = new Team(1,r.nextLong(),"Arizona Cardinals ",nfcWest);
		Team atlanta_falcons = new Team(2,r.nextLong(),"Atlanta Falcons ",nfcSouth);
		Team baltimore_ravens = new Team(3,r.nextLong(),"Baltimore Ravens ",afcNorth);
		Team buffalo_bills = new Team(4,r.nextLong(),"Buffalo Bills ",afcEast);
		Team carolina_panthers = new Team(5,r.nextLong(),"Carolina Panthers ",nfcSouth);
		Team chicago_bears = new Team(6,r.nextLong(),"Chicago Bears ",nfcNorth);
		Team cincinnati_bengals = new Team(7,r.nextLong(),"Cincinnati Bengals ",afcNorth);
		Team cleveland_browns = new Team(8,r.nextLong(),"Cleveland Browns ",afcNorth);
		Team dallas_cowboys = new Team(9,r.nextLong(),"Dallas Cowboys ",nfcEast);
		Team denver_broncos = new Team(10,r.nextLong(),"Denver Broncos ",afcWest);
		Team detroit_lions = new Team(11,r.nextLong(),"Detroit Lions ",nfcNorth);
		Team green_bay_packers = new Team(12,r.nextLong(),"Green Bay Packers ",nfcNorth);
		Team houston_texans = new Team(13,r.nextLong(),"Houston Texans ",afcSouth);
		Team indianapolis_colts = new Team(14,r.nextLong(),"Indianapolis Colts ",afcSouth);
		Team jacksonville_jaguars = new Team(15,r.nextLong(),"Jacksonville Jaguars ",afcSouth);
		Team kansas_city_chiefs = new Team(16,r.nextLong(),"Kansas City Chiefs ",afcWest);
		Team miami_dolphins = new Team(17,r.nextLong(),"Miami Dolphins ",afcEast);
		Team minnesota_vikings = new Team(18,r.nextLong(),"Minnesota Vikings ",nfcNorth);
		Team new_england_patriots = new Team(19,r.nextLong(),"New England Patriots ",afcEast);
		Team new_orleans_saints = new Team(20,r.nextLong(),"New Orleans Saints ",nfcSouth);
		Team new_york_giants = new Team(21,r.nextLong(),"New York Giants ",nfcEast);
		Team new_york_jets = new Team(22,r.nextLong(),"New York Jets ",afcEast);
		Team oakland_raiders = new Team(23,r.nextLong(),"Oakland Raiders ",afcWest);
		Team philadelphia_eagles = new Team(24,r.nextLong(),"Philadelphia Eagles ",nfcEast);
		Team pittsburgh_steelers = new Team(25,r.nextLong(),"Pittsburgh Steelers ",afcNorth);
		Team san_diego_chargers = new Team(26,r.nextLong(),"San Diego Chargers ",afcWest);
		Team san_francisco_49ers = new Team(27,r.nextLong(),"San Francisco 49ers ",nfcWest);
		Team seattle_seahawks = new Team(28,r.nextLong(),"Seattle Seahawks ",nfcWest);
		Team stlouis_rams = new Team(29,r.nextLong(),"St. Louis Rams ",nfcWest);
		Team tampa_bay_buccaneers = new Team(30,r.nextLong(),"Tampa Bay Buccaneers ",nfcSouth);
		Team tennessee_titans = new Team(31,r.nextLong(),"Tennessee Titans ",afcSouth);
		Team washington_redskins = new Team(32,r.nextLong(),"Washington Redskins",nfcEast);
		
		teams = Arrays.asList(arizona_cardinals, atlanta_falcons, baltimore_ravens, buffalo_bills, carolina_panthers, chicago_bears, cincinnati_bengals, cleveland_browns, dallas_cowboys, denver_broncos, detroit_lions, green_bay_packers, houston_texans, indianapolis_colts, jacksonville_jaguars, kansas_city_chiefs, miami_dolphins, minnesota_vikings, new_england_patriots, new_orleans_saints, new_york_giants, new_york_jets, oakland_raiders, philadelphia_eagles, pittsburgh_steelers, san_diego_chargers, san_francisco_49ers, seattle_seahawks, stlouis_rams, tampa_bay_buccaneers, tennessee_titans, washington_redskins);
		teams.sort((a,b)->a.getRandomSeed() > b.getRandomSeed() ? 1 : a.getRandomSeed() < b.getRandomSeed() ? -1 : 0);
	}

	public List<Division> getDivisions() {
		return divisions;
	}

	public List<Conference> getConferences() {
		return conferences;
	}

	public List<Team> getTeams() {
		return teams;
	}

	public Map<Division, Division> getInConferenceTable() {
		return inConferenceTable;
	}

	public Map<Division, Division> getOutOfConferenceTable() {
		return outOfConferenceTable;
	}
	
}
