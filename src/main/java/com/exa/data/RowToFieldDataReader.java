package com.exa.data;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exa.data.config.DataManFactory;
import com.exa.data.config.DataManFactory.DMUSetup;
import com.exa.data.config.utils.DMUtils;
import com.exa.expression.Variable;
import com.exa.expression.VariableContext;
import com.exa.expression.XPOperand;
import com.exa.lang.expression.XALCalculabeValue;
import com.exa.utils.ManagedException;
import com.exa.utils.io.FilesRepositories;
import com.exa.utils.values.BooleanValue;
import com.exa.utils.values.CalculableValue;
import com.exa.utils.values.DecimalValue;
import com.exa.utils.values.IntegerValue;
import com.exa.utils.values.ObjectValue;
import com.exa.utils.values.StringValue;
import com.exa.utils.values.Value;


public class RowToFieldDataReader extends StandardDRWithDSBase<RowToFieldDataReader.Field> {
	public static class Field extends com.exa.data.Field {
		protected Value<?, XPOperand<?>> valueExp;
		
		protected Value<?, XPOperand<?>> ifExp;
		
		protected Value<?, XPOperand<?>> defaultValueExp;

		public Field(String name, String type, Value<?, XPOperand<?>> ifExp, Value<?, XPOperand<?>> valueExp, Value<?, XPOperand<?>> defaultValueExp) {
			super(name, type);
			
			this.ifExp = ifExp;
			this.valueExp = valueExp;
			this.defaultValueExp = defaultValueExp;
		}
	}
	
	protected DataReader<?> drSource = null;
	
	protected Value<?, XPOperand<?>> valueExp;
	
	protected Value<?, XPOperand<?>> defaultIfExp;
	
	protected Value<?, XPOperand<?>> typeExp;
	
	protected Value<?, XPOperand<?>> defaultValueExp;
	
	protected Map<String, Object> values = new LinkedHashMap<>();
	
	public RowToFieldDataReader(String name, ObjectValue<XPOperand<?>> config, FilesRepositories filesRepos, Map<String, XADataSource> dataSources, String defaultDataSource, DMUtils dmu, DMUSetup dmuSetup) {
		super(name, config, filesRepos, dataSources, defaultDataSource, dmu, dmuSetup);
	}

	@Override
	public boolean next() throws DataException {
		
		while(drSource.next()) {
			for(Field field : fields.values()) {
				try {
					
					if(field.ifExp.asBoolean()) {
						Object v = field.valueExp.getValue();
						if(!values.containsKey(field.getName()) || v !=null)
							values.put(field.getName(), v);
					}
				} catch (ManagedException e) {
					throw new DataException(e);
				}
			}
		}
		return drSource.lineVisited() > 0;
	}

	@Override
	public String getString(String fieldName) throws DataException {
		Object res = values.get(fieldName);
		if(res == null) return null;
		return res.toString();
	}

	@Override
	public Date getDate(String fieldName) throws DataException {
		return (Date) values.get(fieldName);
	}

	@Override
	public Double getDouble(String fieldName) throws DataException {
		Number res = (Number) values.get(fieldName);
		if(res == null) return null;
		return res.doubleValue();
	}

	@Override
	public boolean open() throws DataException {
		
		try {
			
			VariableContext variableContext = dmu.getVc();
			
			ObjectValue<XPOperand<?>> ovSource = config.getRequiredAttributAsObjectValue("source");
			
			String type = ovSource.getRequiredAttributAsString("type");
			
			DataManFactory dmf = dmFactories.get(type);
			
			if(dmf == null) throw new ManagedException(String.format("the type %s is unknown", type));
			
			String ds = ovSource.getAttributAsString("dataSource", dmf.getDefaultDataSource());
			DMUtils sourceDmu = dmu.newSubDmu(ds);
			dmuSetup.setup(dmu);
			
			drSource = dmf.getDataReader("source", ovSource, sourceDmu);
			
			variableContext.addVariable("sourceDr", DataReader.class, drSource);
			
			addSourceDRInVC(drSource);
			
			ObjectValue<XPOperand<?>> fm = config.getAttributAsObjectValue("fields");
			
			this.defaultIfExp = fm.getAttribut("if");
			
			this.valueExp = fm.getAttribut("value");
			
			this.defaultValueExp = fm.getAttribut("default");
			
			this.typeExp = fm.getAttribut("type");
			
			String defaultType = null;
			
			if(this.typeExp != null) defaultType = this.typeExp.asString();
			if(defaultType == null) defaultType = "string";
			
			ObjectValue<XPOperand<?>> ovItems = fm.getRequiredAttributAsObjectValue("items");
			Map<String, Value<?,XPOperand<?>>> mpFields = ovItems.getValue();
			
			for(String fname : mpFields.keySet()) {
				Value<?, XPOperand<?>> vlField = mpFields.get(fname);
				
				String fieldType; Value<?, XPOperand<?>> ifExp = null; Value<?, XPOperand<?>> valueExp = null; ; Value<?, XPOperand<?>> defaultValueExp = null;
				BooleanValue<?> blField = vlField.asBooleanValue();
				
				if(blField == null) {
					StringValue<XPOperand<?>> sv = vlField.asStringValue(); 
					IntegerValue<XPOperand<?>> iv = vlField.asIntegerValue();
					BooleanValue<XPOperand<?>> bv = vlField.asBooleanValue();
					DecimalValue<XPOperand<?>> dcv = vlField.asDecimalValue();
					
					if(sv == null && iv == null && bv == null && dcv == null) {
						ObjectValue<XPOperand<?>> ovField = vlField.asRequiredObjectValue();
						
						fieldType = ovField.getAttributAsString("type");
						if(fieldType == null) fieldType = defaultType;
						
						ifExp = ovField.getAttribut("if");
						if(ifExp == null) ifExp = this.defaultIfExp;
						
						valueExp = ovField.getAttribut("value");
						if(valueExp == null) valueExp = this.valueExp;
						
						defaultValueExp = ovField.getAttribut("default");
						if(defaultValueExp == null) defaultValueExp = this.defaultValueExp;
					}
					else {
						fieldType = defaultType;
						valueExp = vlField;
					}
				}
				else {
					fieldType = defaultType;
					valueExp = this.valueExp;
				}
				if(valueExp == null) throw new DataException(String.format("No value expression defined for field %s", fname) );
				fields.put(fname, new Field(fname, fieldType, ifExp == null ? new BooleanValue<>(Boolean.TRUE) : ifExp, valueExp, defaultValueExp));
			}
			
			for(Field field : fields.values()) {
				if(field.defaultValueExp == null) continue;
				
				values.put(field.name, field.defaultValueExp.getValue());
			}
			
			dmu.executeBeforeConnectionActions();
			
			return drSource.open();
		} catch (ManagedException e) {
			throw new DataException(e);
		}
	}
	
	private static void addSourceDRInVC(ObjectValue<XPOperand<?>> ov, DataReader<?> thisDr, DataReader<?> sourceDr) throws ManagedException {
		Map<String, Value<?, XPOperand<?>>> mp = ov.getValue();
		
		for(String propertyName : mp.keySet()) {
			//if("source".equals(propertyName)) continue;
			
			Value<?, XPOperand<?>> vl=mp.get(propertyName);
			
			ObjectValue<XPOperand<?>> vov = vl.asObjectValue();
			if(vov != null) {
				addSourceDRInVC(vov, thisDr, sourceDr);
				continue;
			}
			
			CalculableValue<?, XPOperand<?>> cl = vl.asCalculableValue();
			if(cl == null) continue;
			
			XALCalculabeValue<?> xalCL = (XALCalculabeValue<?>) cl;
			VariableContext vc = xalCL.getVariableContext();
			VariableContext varVc = getVariable(vc, "sourceDr");
			if(varVc != null) 
				varVc.addVariable("sourceDr", DataReader.class, sourceDr);
				
			varVc = getVariable(vc, "this");
			if(varVc != null) 
				varVc.addVariable("this", DataReader.class, thisDr);
			
		}
	}
	
	private static VariableContext getVariable(VariableContext originalVc, String vname) {
		Variable<?> var = null;
		VariableContext vc = originalVc;
		do {
			var = vc.getContextVariable(vname);
			if(var != null) {
				if(var.asXPressionVariable() == null) return null;
				
				originalVc = vc.getParent();
				if(originalVc == null) return null;
			}
			
			vc = vc.getParent();
			
		} while(vc != null);
		
		return originalVc;
	}
	
	private void addSourceDRInVC(DataReader<?> sourceDr) throws ManagedException {
		Map<String, Value<?, XPOperand<?>>> mp = config.getValue();
		
		for(String propertyName : mp.keySet()) {
			if("source".equals(propertyName)) continue;
			
			Value<?, XPOperand<?>> vl=mp.get(propertyName);
			
			ObjectValue<XPOperand<?>> vov = vl.asObjectValue();
			if(vov != null) {
				addSourceDRInVC(vov, this, sourceDr);
				continue;
			}
			
			CalculableValue<?, XPOperand<?>> cl = vl.asCalculableValue();
			if(cl == null) continue;
			
			XALCalculabeValue<?> xalCL = (XALCalculabeValue<?>) cl;
			VariableContext vc = xalCL.getVariableContext();
			VariableContext varVc = getVariable(vc, "sourceDr");
			if(varVc != null) 
				varVc.addVariable("sourceDr", DataReader.class, sourceDr);
				
			varVc = getVariable(vc, "this");
			if(varVc != null) 
				varVc.addVariable("this", DataReader.class, this);
				
		}
	}
	

	@Override
	public void close() throws DataException {
		if(drSource != null) try { drSource.close();} catch(DataException e) { e.printStackTrace();}
		
		dmu.clean();
	}

	@Override
	public boolean isOpen() {
		return drSource != null;
	}

	@Override
	public RowToFieldDataReader cloneDM() throws DataException {
		return new RowToFieldDataReader(name, config, filesRepos, dataSources, defaultDataSource, dmu, dmuSetup);
	}

	@Override
	public int lineVisited() { return drSource.lineVisited(); }

	@Override
	public Integer getInteger(String fieldName) throws DataException {
		Number res = (Number) values.get(fieldName);
		if(res == null) return null;
		return res.intValue();
	}

}
