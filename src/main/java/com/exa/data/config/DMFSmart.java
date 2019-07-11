package com.exa.data.config;

import java.util.HashMap;
import java.util.Map;

import com.exa.data.DataReader;
import com.exa.data.DataWriter;
import com.exa.data.MapReader.MapGetter;
import com.exa.data.config.utils.DMutils;
import com.exa.data.SmartDataReader;
import com.exa.data.XADataSource;
import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.expression.eval.XPEvaluator;
import com.exa.expression.parsing.Parser.UnknownIdentifierValidation;
import com.exa.utils.ManagedException;
import com.exa.utils.io.FilesRepositories;
import com.exa.utils.values.ObjectValue;

public class DMFSmart extends DataManFactory {
	
	private MapGetter mapGetter;

	public DMFSmart(FilesRepositories filesRepos, Map<String, XADataSource> dataSources, String defaultDataSource, MapGetter mapGetter) {
		
		super(filesRepos, dataSources, defaultDataSource, (id, context) -> {
			if("rootDr".equals(id)) return "DataReader";
			if("rootOv".equals(id)) return "ObjectValue";
			String p[] = context.split("[.]");
			if(p.length>=3 && ("this".equals(id) || getDRVariableName(id).equals(p[2]))) return "DataReader";
			if(p.length>=3 && "sourceDr".equals(id)) return "DataReader";
			return null;
		});
		
		this.mapGetter = mapGetter;
	}
	
	public DMFSmart(FilesRepositories filesRepos, Map<String, XADataSource> dataSources, String defaultDataSource) {
		this(filesRepos, dataSources, defaultDataSource, () -> new HashMap<>());
	}
	
	public DMFSmart(FilesRepositories filesRepos, Map<String, XADataSource> dataSources, String defaultDataSource, UnknownIdentifierValidation uiv) {
		super(filesRepos, dataSources, defaultDataSource, uiv);
	}
	
	public DMFSmart(DMFGeneral dmuDmf) {
		super(dmuDmf);
	}

	@Override
	public DataReader<?> getDataReader(String name, ObjectValue<XPOperand<?>> config, XPEvaluator eval, VariableContext variableContext, DMutils dmu) throws ManagedException {
		return new SmartDataReader(name, config, eval, variableContext, filesRepos, dataSources, defaultDataSource, dmu, mapGetter);
	}

	@Override
	public DataWriter<?> getDataWriter(String name, ObjectValue<XPOperand<?>> ovEntity, XPEvaluator eval,
			VariableContext vc, DataReader<?> drSource, DMutils dmu, boolean preventInsertion, boolean preventUpdate) throws ManagedException {
		// TODO Auto-generated method stub
		return null;
	}

}
