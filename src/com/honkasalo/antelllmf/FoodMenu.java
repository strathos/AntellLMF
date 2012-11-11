package com.honkasalo.antelllmf;

public class FoodMenu {
	private String weekTitle;
	private String monday;
	private String tuesday;
	private String wednesday;
	private String thursday;
	private String friday;
	private String saturday;
	private String sunday;
	private String weeksSpecials;
	
	public String getWeekTitle() {
		return weekTitle;
	}
	
	public void setWeekTitle(String weekTitle) {
		this.weekTitle = weekTitle;
	}
	
	public String getWeeksSpecials() {
		return weeksSpecials;
	}
	public void setWeeksSpecials(String weeksSpecials) {
		this.weeksSpecials = weeksSpecials;
	}
	public String getMonday() {
		return monday;
	}
	public void setMonday(String monday) {
		this.monday = monday;
	}
	public String getTuesday() {
		return tuesday;
	}
	public void setTuesday(String tuesday) {
		this.tuesday = tuesday;
	}
	public String getWednesday() {
		return wednesday;
	}
	public void setWednesday(String wednesday) {
		this.wednesday = wednesday;
	}
	public String getThursday() {
		return thursday;
	}
	public void setThursday(String thursday) {
		this.thursday = thursday;
	}
	public String getFriday() {
		return friday;
	}
	public void setFriday(String friday) {
		this.friday = friday;
	}
	public String getSaturday() {
		return saturday;
	}
	public void setSaturday(String saturday) {
		this.saturday = saturday;
	}
	public String getSunday() {
		return sunday;
	}
	public void setSunday(String sunday) {
		this.sunday = sunday;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getWeekTitle()+"\n");
		sb.append(getMonday()+"\n");
		sb.append(getTuesday()+"\n");
		sb.append(getWednesday()+"\n");
		sb.append(getThursday()+"\n");
		sb.append(getFriday()+"\n");
		sb.append(getWeeksSpecials()+"\n");
		return sb.toString();
	}

}
