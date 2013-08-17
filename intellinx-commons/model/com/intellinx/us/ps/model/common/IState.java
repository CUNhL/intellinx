package com.intellinx.us.ps.model.common;

public interface IState {

	public void setCode(String code);

	public String getCode();

	public void setDescription(String description);

	public String getDescription();

	public void setCountry(ICountry country);

}
