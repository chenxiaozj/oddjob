Oddjob-1.1.0
============

Changes in 1.1.0
----------------
- Added a 'forceComplete' action that will force a job to be COMPLETE.
- Added support Proxying Callables in Oddjob.
- The ContextClassLoader is now always set correctly before running a job.
- The repeat job has ben re-written to no longer support schedules just 
  simple repeats.
- Fixed bug with not registering Out Parameters in SQL Job.
- Moved the Struts Webapp to JSF 2.1 with MyFaces 2.1.4 amd JSF AJAX is 
  now used to update just the main sections of the web view.
- The CSS for the Webapp has been improved.
- Fixed bug whereby explorer wouldn't close when Oddjob is killed with 
  Ctrl-C.

Still To Do for 1.1.0
---------------------
- Look at having a start job and a run job. The run job could use
  Oddjobs ComponentResolver to wrap a Runnable or Service. It could be
  structural and expose the child so properties and Log could be viewed.
- Remove Async property from Stop job. A separate trigger can be used
  instead which is much more explicit.

Deferred to A Later Version
---------------------------
- Add Security to the WebApp (with a Read Only role).  
- Include a Jetty Oddball to allow connecting to an Oddjob server from a 
  browser without the need for a separate Servlet Container.
- Improve the AJAX JSF front end to be more AJAXy.
- Introduce the idea of Read only configuration that can't be modified if
  it's been loaded from a non modifiable resource, i.e. from the class path.
- Check a configuration hasn't been modified by someone else before a 
  modification is saved back from designer.
- Add Undo functionality to Oddjob configuration.
- Document conversions. Possibly include conversions in the Reference.
- Improve the <rename> job. Follow Ant's lead of changing to a <move> job and
  copy some of it's feature including; overwrite, force, failonerror, verbose,
  preservelastmodified. Add the ability to back up the moved files like Linux does.
- Introduce a FilterType that can filter files by modified date, created date,
  or match against a regular expression.
- Should structural jobs COMPLETE when they have no child? Need to look at what 
  happens when they are destroyed to ensure they don't complete as their children 
  are removed.

Changes in 1.0.0
----------------
- A new append type has been introduced that allows a file to be appended to.
- Corrected millisecond date format to be yyyy-MM-dd HH:mm:ss.SSS (decimal 
  point not colon).
- Fixed bug with stopping wrapped jobs via thread interrupt.
- Reworked state so components can have different state. Services and structural jobs
  now have different states to Jobs.
- JMX server now has a read only mode.
- JMX server will now use the Platform MBean Server if no URL
  is provided.
- Fixed bug in foreach where @Inject didn't work.
- foreach now has purgeAfter and preLoad properties that introduce the idea
  of an execution window for values.
- Support for opening several Oddjob Explorers from one Oddjob has been added
  by the introduction of a MultiExplorerLauncher job. This is now the default 
  job for the explorer element.
- A DelimitedType has been introduced to support more flexible String to 
  String array conversion than just CSV.
- foreach now provides a file attribute for configuration and it supports the
  Design Inside action for configuration. It was necessary to introduce a
  root foreach element in the inner configuration.
- fix bug where Oddjob leaves configuration input streams open.
- Echo can now echo any array of String using the lines property.
- XML can now provide an ArooaConfiguration that also supports
  inline editing and saving back into the original configuration that
  the xml property of the arooa:configuration type did not.
- foreach supports execution of it's children in parallel.
- Schedules have been refactored. Element names have changed and merged:
    dayOfYear -> yearly
    monthOfYear -> yearly
    dayOfMonth -> monthly, which now has weekOfMonth and dayOfWeek to allow
      things like the last Friday of the month to be specified more easily.
    dayOfWeek ->  weekly
    time -> daily, also introduced a new time schedule that is a single
      once only time.
    dayAfter -> day-after, also introduced day-before to improve scheduling
     around holidays.
- Removed Ids from Values. This is because they over complicated 
  configuration via the GUI. Introduced a new IdentifiableValueType, 
  with tag <identify>, that can used to register a value if required.
- Scheduling jobs (Timer and Retry) that are still ACTIVE go to READY
  if stopped. This ensures they can be restarted without a reset, which had caused
  their schedules to be reset, instead of picking up where they left off.
- Parallel is now completely asynchronous so that it doesn't wait for it's children.
  Waiting for children can still be achieved by nesting in a <state:join> job.
  State transitions are now predictable (seen release notes for version 0.30.0). 
- Fixed problems in <state:cascade> that cause an exception when attempting
  to drag and drop children. 
- Problems with scheduling around Daylight Saving Times have been fixed.
- Changed Echo text property to be XML text rather than an attribute to allow
  multiline echos in one echo job.
- Explorer and Designer have been tidied up. Shortcut keys now work.
  Escape and Ctrl-Enter accelerator keys have been added to dialogs. Node
  selection is more intelligent after a selected node is removed or modified.
  The root node is automatically in focus when the job tree is loaded.
- Logging messages have been tidied up. The job name has been removed from log
  messages and added to the Log4j Mapped Diagnostic Context instead with the
  key 'ojname'.
- Upgraded Apache commons-io to be 2.1
- Upgraded HSQL to 2.2.5
 

Changes in 0.30.0
-----------------
- Properties Panel of Explorer now updates on Job completion.
- Explorer dialog positioning has been improved.
- Job sleep and stop now use State synchronisation as race conditions were
  causing tests to fail.
- Upgraded Ant libs to 1.8.2.
- Oddjob now unpacks into an oddjob directory without version numbers. This 
  is so oddballs can unpack from a zip file easily.
- The Arooa Descriptor format has been simplified.
- Throttling can now be applied to parallel execution to reduce the number
  of simultaneously running jobs.
- Found the bug with dragging configuration with id's. Ids are no longer 'lost'.
- Log4j version has been updated to 1.2.16 and support the TRACE level added.
- Child and Parent Oddjobs can now share properties and values.
- Properties can now override existing properties with the override attribute.
  The environment suffix can only now be used with PropertiesJob now, not
  the PropertiesType.
- Added an Unload Action. This allows a configuration to be unloaded from
  oddjob without using Hard Reset.
- Added an AddJob action. This allows a job to be added to Oddjob without
  the need to use the designer on the parent job, which destroys the 
  state of any jobs already run for that parent.
- Allowed Parallel not to wait for it's children to complete. Not sure if
  this is a good idea - maybe should keep thread driven and event driven
  jobs separate. Also this introduces an uncertainty with the state
  sequence as parallel can now go READY - EXECUTING - READY - EXECUTING
  depending on how quickly the child executes. This will need thinking about
  for the next version.
- Converted Oddjob's date format to ISO 8601 - i.e. yyyy-mm-dd. Unlike the old
  format that used the names of the month, this ensures that an Oddjob 
  configuration created on a JVM in one locale can be used on a JVM with any 
  other locale.
- Converted dayofweek/monthofyear/dayofyear to number formats for the same
  reason.
- A new ConvertType has been introduced to force conversions.
- The <class> element has become <bean>, and <class-for> has become <class>. 
  This seemed more appropriate, especially for Spring users, but I was 
  beginning to regret it after changing a 100 or so tests.
- BufferType now resets it's contents when configured.
- Removed depricated client/server tags. Now only namespaced 
  jmx:client/jmx:server tags are allowed.
- An Input job has been added that allows Oddjob to ask for user input, 
  either from the console or from OddjobExplorer.
- A command line parser for Exec has been implemented which allows for quotes 
  and arguments over multiple lines.
- More Examples have been added to the Reference.
- A mechanism for including XML Files in the reference has been implemented. 
  This allows examples to be wrapped in a unit test.
- Changed the reference to be Descriptor driven. All properties are 
  included whether they are documented or not.
- Changed the reference format to have summary sections for properties and 
  examples.
- Echo job no longer throws an exception when there no text. It prints a 
  blank line instead.
- Fixed bugs with the merging of list types.
- Added conversion from String to String[] that treats string as a CSV.

Changes in 0.29.0
-------------------
- Introduced a new XMLConfigurationType to allow ArooaConfiguration
objects to be created in configurations.
- Changed the foreach job to use an ArooaConfiguration instead of
just text xml.
- Removed xml and input properties from Oddjob as these can now
be set with the XMLConfigurationType.
- Changed the persisting of Oddjobs again. Now Oddjob is persisted
but the last reset is remembered in applied to the child job
before running.
- Allow Values to have ids as well as components.
- Fixed bug in services where they were only applied for a components 
properties.
- Changed class loading to use services instead of the context class loader.
- BufferType can now take a String or List of Strings. It will now also return an 
array of Strings if required.
- Added a BeanReport which produces a simple tabular report on the properties of
beans.
- Changed SQL persistence to use a BLOB data type, and to use the correct
ClassLoader when restoring jobs.
- Added support for Prepared and Callable statements to SQL job.
- Added an FTP Oddball.
- Added loading of user properties via oddball.properites.
- Fixed problems where accelerator keys aren't associated with actions 
in Explorer
- A new mail Oddball has been added.
- Examples now use new Net and Mail Oddballs.
- Arrays now convert to a list.
- SQL Date, Time and Timstamp conversions have been added.
- Date type has a format property.
- BigDecimal conversions added.
- SQLJob changed to cope with scripts.
- SQLJob now has different possible result processor including 
a result sheet and bean capture.
- A simple pipeline processing paradime, called BeanBus, has been added to
support changeable result processors for SQLJob.
- Changed BSF to Java 1.6 javax.scripting. The bsf Oddball has been removed.
- stdout, stderr, logout and tee types added to capture output.
- Provide a BeanView that provides Titles for properties (work in progress).
- Competing for work functionality has been added with GrabJob and Keeper.
- Basic archiving support has been added using the idea of Silhouettes.
- Dynamic definition of beans is now possible using MagicBeanDescriptorFactory.
- Changed to preferring long element names to be hyphenated rather than 
camel case. i.e. filePersister is now file-persister.
- Removed setting of ContextClassLoader for Oddjob except for launch classpath.
- Reworked properties yet again. Now normal java properties can be referenced in
attribute expressions, and will be used first before component properties.
- JMXClient is now LogEnabled.
- The remote structural handler now uses a single notification with all child 
state, which means the JMX client can cope with missed structural events.
- Stopping jobs now waits for a job to stop before returning. Destroying jobs
now calls stop, so that jobs are stopped before they are deleted/dragged etc.
- State locking has been reworked to be stricter. this means the order of events is now 
completely predictable, but the risk of deadlock has been increased. So far however no
deadlocking has been observed.
- Stafeful now include as lastJobStateEvent method.
- state:cascade and state:join jobs have been added.
- Upgraded the Spring Oddball to Spring-3.0.3.
- The Default Executors now uses an unlimited thread pool for immediate 
execution - i.e. Parallel jobs.

Changes in 0.28.0
-------------------
- Allowed Explorer Actions to be pluggable.
- Introduced a Loadable method allows Oddjob, and the foreach job, to be 
loaded without running.
- Changed Resetable interface to include return status.
- Fixed a bug in Timer where the ScheduleContext wasn't reset.
- Changed cut and paste of Oddjob nodes to act like other simple 
  jobs.
- Added a Design Inside action for Oddjob nodes so they can be designed like 
  other jobs.
- Added a check job which is analogous to the unix test command for text and 
numbers.
- Added a modified flag to ConfigurationSession. 
- Oddjob Explorer now indicates the name of the current configuration (Oddjob) in
the title bar with a * when it's modified.
- Oddjob Explorer now warns when closing a session that contains modifications.
- AntJob can now be stopped but this is a real bodge. The log throws an Exception
causing the build to fail!
- Fixed bug in Designer where Value type fields were not updated.
- Cut and Paste Errors in OddjobExplorer now display in a dialog, not the
error log.
- JMX handlers are now pluggable.
- Changed JMX client handling to allow for missing or different 
version ClientHandlers to allow for a miss match between client and server.
- Improved automatic type conversion to allow for the conversion of any super type. 
- Improved conversion of arrays.
- Added general enum conversions.
- Added the ValueFactory interface and conversion for simple value wrappers.
- State now includes a DESTROYED state. Trigger and Mirror now throw an exception
if their watched job moves to this state.
- A JMX Server can now take an environment map which allows security properties
to be set. A simple security ValueFactory has been created that provides
a JMX simple security environment.
- A JMX Client can now take an environment map which allows security credentials
to be set. A username password ValueFactory has been created that provides
the client credentials for JMX security.
- The JMX Client now detects network failure via a heart beat, and causes the client
to enter an EXCEPTION state.
- Job Synchronisation has now been merged with state change which is much
simpler and elegant.
- Split out the code to create more Oddballs, oj-ant, oj-bsf, 
oj-hsql. Created a new oj-assembly project to build them all.
- Added basic autowiring and use it to improve the setting of Executors 
in the timer jobs.

Known Issues
------------
- Setting a remote property does not work on a remote bean which is a
DynaBean, such as VariablesJob. This is probably because the DynaClass 
used for the MBeans doesn't fake properties like the BeanUtils LazyDynaClass 
does.
- Setting remote nested properties doesn't work. Setting a top level property is
fine but with a property such as x.y BeanUtils will bring x across the network
and set y on the local copy.
- An XML namespace can not be removed from designer once it has been introduced.
