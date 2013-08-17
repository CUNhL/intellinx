package com.intellinx.us.ps.implementation.spring.service.network;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.transformer.Transformer;
import org.springframework.util.Assert;

import com.intellinx.us.ps.algorithms.structures.IntervalTree;
import com.intellinx.us.ps.algorithms.structures.IntervalTree.IntervalData;
import com.intellinx.us.ps.model.common.ICountry;
import com.intellinx.us.ps.model.common.IGeoLocation;
import com.intellinx.us.ps.model.common.IState;

/**
 * 
 * @author RenatoM
 * 
 */
public abstract class AbstractGeolocationService implements InitializingBean,
		Transformer {

	private static final Logger LOGGER_PERFORMANCE = LoggerFactory
			.getLogger("org.perf4j.TimingLogger");

	private String when;

	protected IntervalTree<IGeoLocation> intervalTree;

	protected String geoLocationClassName;

	protected String countryClassName;

	protected String lastUpdate;

	protected Date lastUpdateDate;

	protected Class<?> geoLocationClass;

	protected Class<?> countryClass;

	protected Class<?> stateClass;

	protected String getIpExpression;

	protected Expression ipExpression;

	protected String setCountryExpression;

	protected String setGeolocationExpression;

	protected Expression countryExpression;

	protected Expression geolocationExpression;

	protected ExpressionParser parser;

	private static final String POINT = "\\.";

	private static final String SERVICE_NAME = "GeolocationService";

	private static final String SQUARE_OPEN = "\\[";

	private static final String SQUARE_CLOSE = "]";

	protected static final String DOUBLE_QUOTES = "\"";

	protected static final String COMMA = ",";

	protected static final String BLANK = "";

	private static final String UNKNOWN = "UNKNOWN";

	private String stateClassName;

	protected Expression whenExpression;

	private boolean disabled;

	private Resource location;

	private Resource block;

	private IGeoLocation unknownGeolocation;

	private ICountry unknownCountry;

	private IState unknownState;

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.notNull(stateClassName, "The field [stateClassName] is required");

		// Last update
		SimpleDateFormat dateFormat = new SimpleDateFormat("mm/DD/yyyy");
		lastUpdateDate = dateFormat.parse(lastUpdate);

		//

		parser = new SpelExpressionParser();
		ipExpression = parser.parseExpression(getIpExpression);

		if (when != null)
			whenExpression = parser.parseExpression(when);

		if (setCountryExpression != null)
			countryExpression = parser.parseExpression(setCountryExpression);

		if (setGeolocationExpression != null)
			geolocationExpression = parser
					.parseExpression(setGeolocationExpression);

		// Initiate Objects
		geoLocationClass = Class.forName(geoLocationClassName);
		countryClass = Class.forName(countryClassName);
		stateClass = Class.forName(stateClassName);

		//
		// Load Geolocations

		unknownCountry = createUnknownCountry();
		unknownState = createUnknownState();
		unknownGeolocation = createUnknownGeolocation();

		loadGeolocations();

	}

	/**
	 * 
	 */
	@Override
	@ServiceActivator
	public final Message<?> transform(Message<?> message) {

		if (isDisabled())
			return message;

		StopWatch stopWatch = null;

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch = new Slf4JStopWatch(SERVICE_NAME, message.getPayload()
					.getClass().getName(), LOGGER_PERFORMANCE);

		// Check When
		if (whenExpression != null
				&& !whenExpression.getValue(message, Boolean.class))
			return message;

		String ipvalue = (String) ipExpression.getValue(message);

		List<IGeoLocation> candidates = find(ipvalue);

		try {

			ICountry country = null;
			IGeoLocation geoLocation = null;

			if (candidates != null && !candidates.isEmpty()) {

				country = (ICountry) BeanUtils.cloneBean(candidates.get(0)
						.getCountry());

				geoLocation = (IGeoLocation) BeanUtils.cloneBean(candidates
						.get(0));

				geoLocation.setCountry(country);

				// check the state
				IState iState = geoLocation.getState();

				if (iState.getCode() == null
						|| StringUtils.isBlank(iState.getCode())
						|| StringUtils.isEmpty(iState.getCode())) {
					iState.setCode("UNKNOWN");
					iState.setDescription("UNKNOWN");
				}

			} else {
				country = (ICountry) BeanUtils.cloneBean(unknownCountry);
				geoLocation = (IGeoLocation) BeanUtils
						.cloneBean(unknownGeolocation);
			}

			if (countryExpression != null) {
				countryExpression.setValue(message, country);
			}

			if (geolocationExpression != null) {
				geolocationExpression.setValue(message, geoLocation);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (LOGGER_PERFORMANCE.isDebugEnabled())
			stopWatch.stop(SERVICE_NAME);

		return message;
	}

	/**
	 * 
	 * 
	 */
	protected final List<IGeoLocation> find(String toFind) {

		List<IGeoLocation> list = null;
		Long toFindInetAddressLong = ipToInt(toFind);

		IntervalData<IGeoLocation> response = intervalTree
				.query(toFindInetAddressLong);

		if (response != null) {
			list = new ArrayList<IGeoLocation>();
			list.addAll(response.getSet());
		}

		return list;
	}

	/**
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	abstract protected void loadGeolocations() throws IOException,
			InstantiationException, IllegalAccessException;

	/**
	 * 
	 * @param geoLocation
	 * 
	 */
	public void add(IGeoLocation geoLocation) {
		//
		if (geoLocation.getInitialLongValue() == null
				|| geoLocation.getFinalLongValue() == null) {
			calculate(geoLocation);
		}
	}

	/**
	 * 
	 * @param geoLocation
	 * 
	 * 
	 */
	protected final void calculate(IGeoLocation geoLocation) {
		geoLocation.setInitialLongValue(ipToInt(geoLocation.getInitialIp()));
		geoLocation.setFinalLongValue(ipToInt(geoLocation.getFinalIp()));
	}

	/**
	 * 
	 * @param addr
	 * @return
	 */
	protected final Long ipToInt(String addr) {
		addr = addr.replaceAll(SQUARE_OPEN, BLANK).replace(SQUARE_CLOSE, BLANK);
		String[] addrArray = addr.split(POINT);
		long num = 0;
		for (int i = 0; i < addrArray.length; i++) {
			int power = 3 - i;
			num += ((Integer.parseInt(addrArray[i]) % 256 * Math
					.pow(256, power)));
		}
		return num;
	}

	/**
	 * 
	 * @return
	 */
	private IGeoLocation createUnknownGeolocation() {
		//

		IGeoLocation unknownGeolocation = null;

		try {

			unknownGeolocation = (IGeoLocation) geoLocationClass.newInstance();
			unknownGeolocation.setCode(UNKNOWN);
			unknownGeolocation.setCountry(unknownCountry);
			unknownGeolocation.setState(unknownState);
			unknownGeolocation.setDate(lastUpdateDate);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return unknownGeolocation;

	}

	/**
	 * 
	 * @return
	 */
	private IState createUnknownState() {
		return createUnknownState(unknownCountry);
	}

	/**
	 * 
	 * @return
	 */
	private IState createUnknownState(ICountry country) {

		IState unknownState = null;

		try {
			// Create Unkown state
			unknownState = (IState) stateClass.newInstance();
			unknownState.setCode(UNKNOWN);
			unknownState.setDescription("Unknown");
			unknownState.setCountry(country);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return unknownState;

	}

	/**
	 * 
	 * @return
	 */
	private ICountry createUnknownCountry() {

		ICountry unknownCountry = null;

		try {
			//
			unknownCountry = (ICountry) countryClass.newInstance();
			unknownCountry.setCode(UNKNOWN);
			unknownCountry.setDescription("Unknown");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return unknownCountry;

	}

	public String getGeoLocationClassName() {
		return geoLocationClassName;
	}

	public void setGeoLocationClassName(String geoLocationClassName) {
		this.geoLocationClassName = geoLocationClassName;
	}

	public String getCountryClassName() {
		return countryClassName;
	}

	public void setCountryClassName(String countryClassName) {
		this.countryClassName = countryClassName;
	}

	public String getGetIpExpression() {
		return getIpExpression;
	}

	public void setGetIpExpression(String getIpExpression) {
		this.getIpExpression = getIpExpression;
	}

	public String getSetCountryExpression() {
		return setCountryExpression;
	}

	public void setSetCountryExpression(String setCountryExpression) {
		this.setCountryExpression = setCountryExpression;
	}

	public String getSetGeolocationExpression() {
		return setGeolocationExpression;
	}

	public void setSetGeolocationExpression(String setGeolocationExpression) {
		this.setGeolocationExpression = setGeolocationExpression;
	}

	public String getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(String lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public String getWhen() {
		return when;
	}

	public void setWhen(String when) {
		this.when = when;
	}

	public String getStateClassName() {
		return stateClassName;
	}

	public void setStateClassName(String stateClassName) {
		this.stateClassName = stateClassName;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public final Resource getLocation() {
		return location;
	}

	public final void setLocation(Resource location) {
		this.location = location;
	}

	public final Resource getBlock() {
		return block;
	}

	public final void setBlock(Resource block) {
		this.block = block;
	}

}
