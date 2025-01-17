package com.exa.data.config;

import java.util.Map;


import com.exa.data.DataReader;
import com.exa.data.DataWriter;
import com.exa.data.XADataSource;
import com.exa.data.config.utils.DMUtils;
import com.exa.data.sql.SQLDataReader;
import com.exa.data.sql.SQLDataWriter;

import com.exa.expression.XPOperand;

import com.exa.expression.parsing.Parser.UnknownIdentifierValidation;
import com.exa.utils.ManagedException;
import com.exa.utils.io.FilesRepositories;
import com.exa.utils.values.ObjectValue;


public class DMFSql extends DataManFactory {
	
	public DMFSql(FilesRepositories filesRepos, Map<String, XADataSource> dataSources, String defaultDataSource, DMUSetup dmuSetup) {
		super(filesRepos, dataSources, defaultDataSource, dmuSetup);

	}
	
	public DMFSql(FilesRepositories filesRepos, Map<String, XADataSource> dataSources, String defaultDataSource, DMUSetup dmuSetup, UnknownIdentifierValidation uiv) {
		super(filesRepos, dataSources, defaultDataSource, dmuSetup, uiv);
	}

	public DMFSql(DMFGeneral dmuDmf) {
		super(dmuDmf);
	}

	@Override
	public DataReader<?> getDataReader(String name, ObjectValue<XPOperand<?>> ovEntity, DMUtils dmu) throws ManagedException {
		
		/*String dsName = ovEntity.getAttributAsString("dataSource");
		
		if(dsName == null) dsName = defaultDataSource;
		
		if(dsName == null) throw new ManagedException(String.format("No data source provided."));
		
		XADataSource xaDS = dataSources.get(dsName);
		if(xaDS == null) throw new ManagedException(String.format("The data source %s specified is not present.", dsName));
		
		XASQLDataSource xasqlds = xaDS.asXASQLDataSource();
		if(xasqlds == null) throw new ManagedException(String.format("The data source %s specified should be sql type.", dsName));*/
		
		DataReader<?> dr = new SQLDataReader(name, ovEntity, dmu);
		
		return dr;
	}

	@Override
	public DataWriter<?> getDataWriter(String name, ObjectValue<XPOperand<?>> ovEntity, DataReader<?> drSource, DMUtils dmu, boolean preventInsertion, boolean preventUpdate) throws ManagedException {
		/*String dsName = ovEntity.getAttributAsString("dataSource");
		
		if(dsName == null) dsName = defaultDataSource;
		
		if(dsName == null) throw new ManagedException(String.format("No data source provided."));
		
		XADataSource xaDS = dataSources.get(dsName);
		if(xaDS == null) throw new ManagedException(String.format("The data source %s specified is not present.", dsName));
		
		XASQLDataSource xasqlds = xaDS.asXASQLDataSource();
		if(xasqlds == null) throw new ManagedException(String.format("The data source %s specified should be sql type.", dsName));
		
		DataSource ds = xasqlds.getDataSource();
		if(ds == null) throw new ManagedException(String.format("The data source %s specified is not present.", dsName));*/
		
		DataWriter<?> dw = new SQLDataWriter(name, drSource, ovEntity, dmu, preventInsertion, preventUpdate);
		
		return dw;
	}

}
