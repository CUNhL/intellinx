package com.intellinx.us.ps.model.common;

import java.util.Date;

/**
 * 
 * @author RenatoM
 * 
 */
public interface IGeoLocation {

	public String getCode();

	public void setCode(String code);

	public Date getDate();

	public void setDate(Date date);

	public ICountry getCountry();

	public void setCountry(ICountry country);

	public java.lang.String getInitialIp();

	public void setInitialIp(String initialIp);

	public java.lang.String getFinalIp();

	public void setFinalIp(String finalIp);

	public Long getInitialLongValue();

	public void setInitialLongValue(Long initialLongValue);

	public Long getFinalLongValue();

	public void setFinalLongValue(Long finalLongValue);

	public IState getState();

	public void setState(IState state);

	public String getCity();

	public void setCity(String city);

	public String getPostalCode();

	public void setPostalCode(String postalCode);

	public Double getLatitude();

	public void setLatitude(Double latitude);

	public Double getLongitude();

	public void setLongitude(Double longitude);

	public String getMetroCode();

	public void setMetroCode(String metroCode);

	public String getAreaCode();

	public void setAreaCode(String areaCode);

}
