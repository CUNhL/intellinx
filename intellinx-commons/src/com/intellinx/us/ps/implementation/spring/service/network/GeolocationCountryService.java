package com.intellinx.us.ps.implementation.spring.service.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.integration.transformer.Transformer;

import com.intellinx.us.ps.algorithms.structures.IntervalTree;
import com.intellinx.us.ps.algorithms.structures.IntervalTree.IntervalData;
import com.intellinx.us.ps.model.common.ICountry;
import com.intellinx.us.ps.model.common.IGeoLocation;

/**
 * 
 * @author RenatoM
 * 
 */
public class GeolocationCountryService extends AbstractGeolocationService
		implements InitializingBean, Transformer {

	private Resource resource;

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

		File file = resource.getFile();

		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);

		String line = null;

		while ((line = bufferedReader.readLine()) != null) {

			IGeoLocation geoLocation = (IGeoLocation) geoLocationClass
					.newInstance();

			geoLocation.setDate(lastUpdateDate);

			ICountry country = (ICountry) countryClass.newInstance();

			String[] columns = line.split(",");

			//
			geoLocation.setInitialIp(columns[0]
					.replaceAll(DOUBLE_QUOTES, BLANK));
			geoLocation.setFinalIp(columns[1].replaceAll(DOUBLE_QUOTES, BLANK));

			//
			country.setCode(columns[4].replaceAll(DOUBLE_QUOTES, BLANK));
			country.setDescription(columns[5].replaceAll(DOUBLE_QUOTES, BLANK));
			geoLocation.setCountry(country);

			//
			calculate(geoLocation);

			//
			IntervalData<IGeoLocation> intervalData = new IntervalData<IGeoLocation>(
					geoLocation.getInitialLongValue(),
					geoLocation.getFinalLongValue(), geoLocation);
			intervals.add(intervalData);

		}

		intervalTree = new IntervalTree<IGeoLocation>(intervals);
		
		bufferedReader.close();

	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

}
