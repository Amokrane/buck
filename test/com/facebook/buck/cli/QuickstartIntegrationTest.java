/*
 * Copyright 2013-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.facebook.buck.testutil.integration.DebuggableTemporaryFolder;
import com.facebook.buck.testutil.integration.ProjectWorkspace;
import com.facebook.buck.testutil.integration.ProjectWorkspace.ProcessResult;
import com.facebook.buck.testutil.integration.TestDataHelper;
import com.facebook.buck.util.ProjectFilesystem;
import com.google.common.base.Joiner;
import com.google.common.io.Files;

import org.junit.Rule;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Integration test for the {@code buck quickstart} command.
 */
public class QuickstartIntegrationTest {

  @Rule
  public DebuggableTemporaryFolder quickstartDirectory = new DebuggableTemporaryFolder();

  @Rule
  public DebuggableTemporaryFolder destDir = new DebuggableTemporaryFolder();

  /**
   * Test that project is created when it is given various parameters.
   */
  @Test
  public void testQuickstartCreatesProject() throws CmdLineException, IOException {
    ProjectWorkspace quickstartWorkspace = TestDataHelper.createProjectWorkspaceForScenario(
        this, "empty_project", quickstartDirectory);
    quickstartWorkspace.setUp();

    // looks at local.properties, ANDROID_SDK, and ANDROID_HOME
    File androidSdk = AbstractCommandOptions.findAndroidSdkDir(new ProjectFilesystem(new File("."))).orNull();

    ProcessResult result = quickstartWorkspace.runBuckCommand("quickstart",
        "--dest-dir",
        destDir.getRoot().getAbsolutePath(),
        "--android-sdk",
        androidSdk.getAbsolutePath());

    result.assertExitCode(0);

    quickstartWorkspace.verify();

    File readme = new File(destDir.getRoot(), "README.md");
    assertTrue("`buck quickstart` should create a README file.", readme.isFile());
    assertEquals(
        "`buck quickstart` should output the contents of the README file to standard output.",
        Files.toString(readme, StandardCharsets.UTF_8),
        result.getStdout()
    );

    File localProp = new File(destDir.getRoot(), "local.properties");
    assertTrue("`buck quickstart` should create a local.properties file.", localProp.isFile());
    assertEquals(
      "`buck quickstart` should put the Android SDK in the local.properties file.",
      "sdk.dir=" + androidSdk + "\n",
      Files.toString(localProp, StandardCharsets.UTF_8)
    );

    ProjectWorkspace targetsWorkspace = new ProjectWorkspace(destDir.getRoot(), destDir);
    targetsWorkspace.setUp();

    // We can't test building if the user does not have an Android SDK. First, test targets, since
    // it does not have that dependency.
    result = targetsWorkspace.runBuckCommand("targets");

    result.assertExitCode(0);

    targetsWorkspace.verify();

    assertEquals(
      "`buck targets` should display a list of targets.",
      Joiner.on('\n').join("//apps/myapp:app",
        "//apps/myapp:app#aapt_package",
        "//apps/myapp:app#uber_r_dot_java",
        "//apps/myapp:debug_keystore",
        "//apps/myapp:project_config",
        "//java/com/example/activity:activity",
        "//java/com/example/activity:activity#dex",
        "//java/com/example/activity:project_config",
        "//res/com/example/activity:project_config",
        "//res/com/example/activity:res") + "\n",
      result.getStdout());

    result = targetsWorkspace.runBuckCommand("build", "app");

    result.assertExitCode(0);

    targetsWorkspace.verify();

    File buckOut = targetsWorkspace.getFile("buck-out");
    assertTrue("`buck build` should create a buck-out directory.", buckOut.isDirectory());
  }
}
