package sample.plugin;


import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;

import org.junit.Rule;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.File;

public class StopListMojoTest {
    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {

        }

        @Override
        protected void after() {
        }
    };

    @Test
    public void testSomething() throws Exception {
        /*File pom = new File( "target/test-classes/project-to-test/" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        StopListMojo myMojo = ( StopListMojo ) rule.lookupConfiguredMojo( pom, "touch" );
        assertNotNull( myMojo );
        myMojo.execute();

        File outputDirectory = ( File ) rule.getVariableValueFromObject( myMojo, "outputDirectory" );
        assertNotNull( outputDirectory );
        assertTrue( outputDirectory.exists() );

        File touch = new File( outputDirectory, "touch.txt" );
        assertTrue( touch.exists() );*/
        File pom = new File("target/test-classes/project-to-test/");
        assertNotNull(pom);
        assertTrue(pom.exists());
        StopListMojo myMojo = (StopListMojo) rule.lookupConfiguredMojo(pom, "check");
        myMojo.execute();

        System.out.println(myMojo.getTest());
        for (String exclude : myMojo.getExclude()) {
            System.out.println(exclude);
        }
    }

}

