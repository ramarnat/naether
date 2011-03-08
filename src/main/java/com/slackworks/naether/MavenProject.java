package com.slackworks.naether;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
   *
 * http://www.apache.org/licenses/LICENSE-2.0
   *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Java SE
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Apache Maven
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

// Codehause Plexus
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maven Project Model
 * 
 * @author Michael Guymon
 * 
 */
public class MavenProject {

	private static Logger log = LoggerFactory.getLogger(MavenProject.class);

	private Model mavenModel;
	private Pattern propertyPattern = Pattern.compile("^\\$\\{(.+)\\}$");

	private boolean allowCompileScope = true;
	private boolean allowRuntimeScope = true;
	private boolean allowTestScope = false;
	private boolean allowSystemScope = true;
	private boolean allowProvidedScope = false;

	/**
	 * New Instance
	 */
	public MavenProject() {
		setMavenModel( new Model() );
	}

	/**
	 * New Instance loading Maven pom
	 * 
	 * @param pomPath
	 *            String path
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	public MavenProject(String pomPath) throws FileNotFoundException, IOException, XmlPullParserException {
		loadPOM(pomPath);
	}

	/**
	 * Load Maven pom
	 * 
	 * @param pomPath
	 *            String path
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	public void loadPOM(String pomPath) throws FileNotFoundException, IOException, XmlPullParserException {
		log.debug("Loading pom {}", pomPath);
		MavenXpp3Reader reader = new MavenXpp3Reader();
		setMavenModel(reader.read(new BufferedReader(new FileReader(new File( pomPath )))));
	}

	/**
	 * Get version of Maven project
	 * 
	 * @return String
	 */
	public String getVersion() {
		return getMavenModel().getVersion();
	}
	
	public void setVersion( String version ) {
		getMavenModel().setVersion( version );
	}
	
	public String getArtifactId() {
		return getMavenModel().getArtifactId();
	}
	
	public void setArtifactId( String artifactId ) {
		getMavenModel().setArtifactId( artifactId );
	}
	
	public String getGroupId() {
		return getMavenModel().getGroupId();
	}
	
	public void setGroupId( String groupId ) {
		getMavenModel().setGroupId( groupId );
	}
	
	public String getType() {
		return getMavenModel().getPackaging();
	}
	
	public void setType( String type ) {
		getMavenModel().setPackaging( type );
	}
	
	public void setNotation( String notation ) {
		Map<String, String> notationMap = Notation.parse( notation );
		this.setGroupId( notationMap.get( "groupId") );
		this.setArtifactId( notationMap.get( "artifactId" ) );
		this.setType( notationMap.get( "type") );
		this.setVersion( notationMap.get( "version" ) );
	}

	/**
	 * Get List of {@link Dependency} for Maven Project
	 * 
	 * @return List<Dependency>
	 */
	public List<Dependency> getDependencies() {
		return getDependencies(true);
	}

	/**
	 * Get List of {@link Depedencies} for the Maven Project, with boolean to
	 * substitute Project Properties.
	 * 
	 * @param substituteProperties
	 *            boolean
	 * @return List<Dependency>
	 */
	public List<Dependency> getDependencies(boolean substituteProperties) {
		List<Dependency> dependencies = new ArrayList<Dependency>();

		List<String> scopes = this.allowedScopes();

		log.info("Allowed Maven Scopes: {}", scopes);

		// Substitute Properties
		if (substituteProperties) {
			// XXX: There has to be a way maven handles this automatically
			for (Dependency dependency : getMavenModel().getDependencies()) {

				// Check that scope of the Dependency has been marked allowed
				if (scopes.indexOf(dependency.getScope()) >= 0) {

					String artifactId = substituteProperty(dependency.getArtifactId());
					String groupId = substituteProperty(dependency.getGroupId());
					String version = substituteProperty(dependency.getVersion());

					dependency.setArtifactId(artifactId);
					dependency.setGroupId(groupId);
					dependency.setVersion(version);
					dependencies.add(dependency);
				}
			}

		// Keep vals
		} else {
			for (Dependency dependency : getMavenModel().getDependencies()) {
				// Check that scope of the Dependency has been marked allowed
				if (scopes.indexOf(dependency.getScope()) >= 0) {
					dependencies.add(dependency);
				}
			}
			;
		}

		return dependencies;
	}

	/**
	 * Return {@link List<String>} of dependencies in the format of
	 * {@code groupId:artifactId:packageType:version}
	 * 
	 * @return List<String>
	 */
	public List<String> getDependenciesNotation() {
		return getDependenciesNotation(true);
	}

	/**
	 * Get List<String> of dependencies in format of groupId:artifactId:packageType:version
	 * 
	 * @param substituteProperties
	 *            boolean
	 * @return List<String>
	 */
	public List<String> getDependenciesNotation(boolean substituteProperties) {
		List<String> notations = new ArrayList<String>();

		for (Dependency dependency : getDependencies(substituteProperties)) {
			notations.add(Notation.generate(dependency));
		}

		return notations;
	}
	
	public void addDependency( Dependency dependency) {
		mavenModel.addDependency( dependency );
	}
	
	public void addDependency( String notation ) {
		Map<String, String> notationMap = Notation.parse( notation );
		Dependency dependency = new Dependency();
		dependency.setGroupId( notationMap.get( "groupId" ) );
		dependency.setArtifactId( notationMap.get( "artifactId") );
		dependency.setType( notationMap.get( "type" ) );
		dependency.setVersion( notationMap.get( "version" ) );
		mavenModel.addDependency( dependency );
	}

	/**
	 * Set the Maven {@link Model}
	 * 
	 * @param mavenModel {@link Model}
	 */
	public void setMavenModel(Model mavenModel) {
		this.mavenModel = mavenModel;
	}

	/**
	 * Get the Maven {@link Model}
	 * 
	 * @return {@link Model}
	 */
	public Model getMavenModel() {
		return mavenModel;
	}

	/**
	 * Substitute a Maven Property expression, i.e. ${aetherVersion}, to its
	 * corresponding Maven pom definition, i.e. 1.11 from
	 * {@code <aetherVersion>1.11</aetherVersion>}
	 * 
	 * @param field
	 * @return
	 */
	private String substituteProperty(String field) {
		String property = null;
		Matcher matcher = propertyPattern.matcher(field);
		while (matcher.find()) {
			property = matcher.group(1);
		}

		if (property != null) {
			return this.getMavenModel().getProperties().getProperty(property);
		} else {
			return field;
		}
	}

	/**
	 * List<String> of scopes allows scopes.
	 * 
	 * Possible scopes: compile, provided, runtime, system, test
	 * 
	 * The compile scope is represented by {@code compile} and {@code null}
	 * 
	 * @return List<String>
	 */
	private List<String> allowedScopes() {
		List<String> scopes = new ArrayList<String>();

		if (this.allowCompileScope) {
			scopes.add("compile");
			scopes.add(null);
		}

		if (this.allowProvidedScope) {
			scopes.add("provided");
		}

		if (this.allowRuntimeScope) {
			scopes.add("runtime");
		}

		if (this.allowSystemScope) {
			scopes.add("system");
		}

		if (this.allowTestScope) {
			scopes.add("test");
		}

		return scopes;
	}

	public void setAllowProvidedScope(boolean allowProvidedScope) {
		this.allowProvidedScope = allowProvidedScope;
	}

	public boolean isAllowProvidedScope() {
		return allowProvidedScope;
	}

	private void setAllowSystemScope(boolean allowSystemScope) {
		this.allowSystemScope = allowSystemScope;
	}

	private boolean isAllowSystemScope() {
		return allowSystemScope;
	}

	public void setAllowTestScope(boolean allowTestScope) {
		this.allowTestScope = allowTestScope;
	}

	public boolean isAllowTestScope() {
		return allowTestScope;
	}

	public void setAllowRuntimeScope(boolean allowRuntimeScope) {
		this.allowRuntimeScope = allowRuntimeScope;
	}

	public boolean isAllowRuntimeScope() {
		return allowRuntimeScope;
	}

	public void setAllowCompileScope(boolean allowCompileScope) {
		this.allowCompileScope = allowCompileScope;
	}

	public boolean isAllowCompileScope() {
		return allowCompileScope;
	}
	
	public void writePom( String filePath ) throws IOException {
		Writer writer = new BufferedWriter(new FileWriter(filePath));
		MavenXpp3Writer pomWriter = new MavenXpp3Writer();
		pomWriter.write( writer, this.mavenModel );
	}
}
