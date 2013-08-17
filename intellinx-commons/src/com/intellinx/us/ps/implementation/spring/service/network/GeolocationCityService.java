package com.intellinx.us.ps.implementation.spring.service.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.transformer.Transformer;

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
public class GeolocationCityService extends AbstractGeolocationService
		implements InitializingBean, Transformer {

	private final static Logger LOGGER = LoggerFactory
			.getLogger(GeolocationCityService.class);

	/**
	 * 
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		super.afterPropertiesSet();

	}

	/**
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	protected void loadGeolocations() throws IOException,
			InstantiationException, IllegalAccessException {

		List<IntervalTree.IntervalData<IGeoLocation>> intervals = new ArrayList<IntervalTree.IntervalData<IGeoLocation>>();

		File file = getLocation().getFile();

		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);

		String line = null;

		// skip 2 lines
		bufferedReader.readLine();
		bufferedReader.readLine();

		Map<String, IGeoLocation> map = new HashMap<String, IGeoLocation>();

		while ((line = bufferedReader.readLine()) != null) {

			IGeoLocation geoLocation = (IGeoLocation) geoLocationClass
					.newInstance();

			geoLocation.setDate(lastUpdateDate);

			String[] columns = line.split(COMMA);

			// 0, 1, 2, 3, 4, 5, 6, 7, 8
			// locId,country,state,city,postalCode,latitude,longitude,metroCode,areaCode

			// 0 - locId
			geoLocation.setCode(columns[0].replaceAll(DOUBLE_QUOTES, BLANK));

			// 1 - Country
			ICountry country = (ICountry) countryClass.newInstance();
			country.setCode(columns[1].replaceAll(DOUBLE_QUOTES, BLANK));
			geoLocation.setCountry(country);

			// 2 - State
			IState state = (IState) stateClass.newInstance();
			state.setCode(columns[2].replaceAll(DOUBLE_QUOTES, BLANK));
			state.setCountry(country);
			geoLocation.setState(state);

			// 3 - City
			geoLocation.setCity(columns[3].replaceAll(DOUBLE_QUOTES, BLANK));

			// 4 - Postal Code
			geoLocation.setPostalCode(columns[4].replaceAll(DOUBLE_QUOTES,
					BLANK));

			// 5 - latitude
			geoLocation.setLatitude(Double.valueOf(columns[5].replaceAll(
					DOUBLE_QUOTES, BLANK)));

			// 6 - longitude
			geoLocation.setLongitude(Double.valueOf(columns[6].replaceAll(
					DOUBLE_QUOTES, BLANK)));

			// 7 - metro code
			if (columns.length > 7)
				geoLocation.setMetroCode(columns[7].replaceAll(DOUBLE_QUOTES,
						BLANK));

			// 8 - Area Code
			if (columns.length > 8)
				geoLocation.setAreaCode(columns[8].replaceAll(DOUBLE_QUOTES,
						BLANK));

			if (LOGGER.isDebugEnabled())
				LOGGER.debug("New Geolocation [" + geoLocation.getCode()
						+ "] Added");
			map.put(geoLocation.getCode(), geoLocation);

		}

		//

		file = getBlock().getFile();
		fileReader.close();
		fileReader = new FileReader(file);
		bufferedReader = new BufferedReader(fileReader);
		line = null;

		// skip 2 lines
		bufferedReader.readLine();
		bufferedReader.readLine();

		while ((line = bufferedReader.readLine()) != null) {

			String[] columns = line.split(",");

			// startIpNum,endIpNum,locId
			IGeoLocation geoLocation = map.get(columns[2].replaceAll(
					DOUBLE_QUOTES, BLANK));

			if (geoLocation != null) {

				geoLocation.setInitialLongValue(new Long(columns[0].replaceAll(
						DOUBLE_QUOTES, BLANK)));
				geoLocation.setFinalLongValue(new Long(columns[1].replaceAll(
						DOUBLE_QUOTES, BLANK)));

				IntervalData<IGeoLocation> intervalData = new IntervalData<IGeoLocation>(
						geoLocation.getInitialLongValue(),
						geoLocation.getFinalLongValue(), geoLocation);
				intervals.add(intervalData);

			}

		}
		
		
		bufferedReader.close();
		fileReader.close();

		intervalTree = new IntervalTree<IGeoLocation>(intervals);

	}

}
