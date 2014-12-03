package eu.stratosphere.peel.analyser.Parser;

import eu.stratosphere.peel.analyser.Model.System;
import eu.stratosphere.peel.analyser.Util.HibernateUtil;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.hibernate.Session;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Date;

public class ParserSparkTest extends TestCase{

    private ExperimentRun experimentRun;
    private Task task;
    private Session session = null;

    //remember to close session!
    protected void setUp() throws Exception{
        HibernateUtil.resetDatabase();

        String experimentSuiteName = "kmeans-mllib.dop80.run01";
        String experimentName = "kmeans-mllib.dob80";
        int experimentRuns = 5;
        int experimentRunRun = 1;

        try {
            //create session
            session = HibernateUtil.getSessionFACTORY().openSession();
            session.beginTransaction();

            //create System
            System system = new System();
            system.setName("spark");
            session.save(system);

            //create Experiment Suite
            ExperimentSuite experimentSuite = new ExperimentSuite();
            experimentSuite.setName(experimentSuiteName);
            session.save(experimentSuite);

            //create Experiment and connect it to ExperimentSuite
            Experiment experiment = new Experiment();
            experiment.setExperimentSuite(experimentSuite);
            experiment.setName(experimentName);
            experiment.setRuns(experimentRuns);
            experiment.setSystem(system);
            system.getExperimentSet().add(experiment);
            session.save(experiment);
            experimentSuite.getExperimentSet().add(experiment);

            //create ExperimentRun and add it to Experiment
            experimentRun = new ExperimentRun();
            experimentRun.setExperiment(experiment);
            experimentRun.setRun(experimentRunRun);
            session.save(experimentRun);
            experiment.getExperimentRunSet().add(experimentRun);

            task = new Task();
            task.setExperimentRun(experimentRun);
            task.setTaskType("ResultTask");
            experimentRun.getTaskSet().add(task);
            session.save(task);

            //commit the transaction
            session.getTransaction().commit();
            session.close();        //close session!
        } catch (Exception e){
            if(session != null) {
                session.getTransaction().rollback();
            }
            throw e;
        }
    }

    @Test
    public void testSparkParser()throws Exception{
        session = HibernateUtil.getSessionFACTORY().openSession();

        //setup Mock and Test Data
        String input = "{\"Event\":\"SparkListenerTaskEnd\",\"Stage ID\":0,\"Stage Attempt ID\":0,\"Task Type\":\"ResultTask\",\"Task End Reason\":{\"Reason\":\"Success\"},\"Task Info\":{\"Task ID\":19,\"Index\":21,\"Attempt\":0,\"Launch Time\":1414094710001,\"Executor ID\":\"7\",\"Host\":\"wally105.cit.tu-berlin.de\",\"Locality\":\"NODE_LOCAL\",\"Speculative\":false,\"Getting Result Time\":0,\"Finish Time\":1414094743584,\"Failed\":false,\"Accumulables\":[]},\"Task Metrics\":{\"Host Name\":\"wally105.cit.tu-berlin.de\",\"Executor Deserialize Time\":319,\"Executor Run Time\":33012,\"Result Size\":2562,\"JVM GC Time\":3215,\"Result Serialization Time\":0,\"Memory Bytes Spilled\":0,\"Disk Bytes Spilled\":0,\"Shuffle Read Metrics\":{\"Shuffle Finish Time\":-1,\"Remote Blocks Fetched\":0,\"Local Blocks Fetched\":0,\"Fetch Wait Time\":0,\"Remote Bytes Read\":0},\"Input Metrics\":{\"Data Read Method\":\"Memory\",\"Bytes Read\":136478871},\"Updated Blocks\":[{\"Block ID\":\"rdd_2_21\",\"Status\":{\"Storage Level\":{\"Use Disk\":false,\"Use Memory\":true,\"Use Tachyon\":false,\"Deserialized\":true,\"Replication\":1},\"Memory Size\":136478871,\"Tachyon Size\":0,\"Disk Size\":0}},{\"Block ID\":\"rdd_3_21\",\"Status\":{\"Storage Level\":{\"Use Disk\":false,\"Use Memory\":true,\"Use Tachyon\":false,\"Deserialized\":true,\"Replication\":1},\"Memory Size\":56197191,\"Tachyon Size\":0,\"Disk Size\":0}}]}}";
        BufferedReader reader = EasyMock.createMock(BufferedReader.class);
        EasyMock.expect(reader.readLine()).andReturn(input);
        EasyMock.expect(reader.readLine()).andReturn(null);
        EasyMock.replay(reader);

        Integer taskInstanceNumber = 19;
        Date launch = new Date(1414094710001L);
        Date finished = new Date(1414094743584L);
        ParserSpark parserSpark = new ParserSpark(experimentRun, session);
        parserSpark.parse(reader);

        session.close();
        Task taskResult = experimentRun.getTaskSet().iterator().next();
        TaskInstance taskInstanceResult = taskResult.getTaskInstances().iterator().next();
        assertEquals("ResultTask", taskResult.getTaskType());
        assertEquals(taskInstanceNumber, taskInstanceResult.getSubTaskNumber());
        assertEquals(launch, taskInstanceResult.getEventByName("Launch").getValueTimestamp());
        assertEquals(finished, taskInstanceResult.getEventByName("Finished").getValueTimestamp());
    }

    @Test
    public void testSparkParser2()throws Exception{
        session = HibernateUtil.getSessionFACTORY().openSession();

        //setup Mock and Test Data
        String input = "{\"Event\":\"SparkListenerTaskEnd\",\"Stage ID\":13,\"Stage Attempt ID\":0,\"Task Type\":\"ShuffleMapTask\",\"Task End Reason\":{\"Reason\":\"Success\"},\"Task Info\":{\"Task ID\":2885,\"Index\":3,\"Attempt\":0,\"Launch Time\":1414094815615,\"Executor ID\":\"5\",\"Host\":\"wally102.cit.tu-berlin.de\",\"Locality\":\"PROCESS_LOCAL\",\"Speculative\":false,\"Getting Result Time\":0,\"Finish Time\":1414094817066,\"Failed\":false,\"Accumulables\":[]},\"Task Metrics\":{\"Host Name\":\"wally102.cit.tu-berlin.de\",\"Executor Deserialize Time\":12,\"Executor Run Time\":1426,\"Result Size\":2194,\"JVM GC Time\":60,\"Result Serialization Time\":0,\"Memory Bytes Spilled\":0,\"Disk Bytes Spilled\":0,\"Shuffle Write Metrics\":{\"Shuffle Bytes Written\":1120,\"Shuffle Write Time\":52525},\"Input Metrics\":{\"Data Read Method\":\"Memory\",\"Bytes Read\":57050184}}}";
        BufferedReader reader = EasyMock.createMock(BufferedReader.class);
        EasyMock.expect(reader.readLine()).andReturn(input);
        EasyMock.expect(reader.readLine()).andReturn(null);
        EasyMock.replay(reader);

        Integer subTaskNumber = 2885;
        Date launch = new Date(1414094815615L);
        Date finished = new Date(1414094817066L);
        ParserSpark parserSpark = new ParserSpark(experimentRun, session);
        parserSpark.parse(reader);

        session.close();
        Task taskResult = experimentRun.taskByTaskType("ShuffleMapTask");
        TaskInstance taskInstanceResult = taskResult.taskInstanceBySubtaskNumber(subTaskNumber);
        assertEquals("ShuffleMapTask", taskResult.getTaskType());
        assertEquals(subTaskNumber, taskInstanceResult.getSubTaskNumber());
        assertEquals(launch, taskInstanceResult.getEventByName("Launch").getValueTimestamp());
        assertEquals(finished, taskInstanceResult.getEventByName("Finished").getValueTimestamp());
    }

    @Test
    public void testEntireFile() throws Exception {
        session = HibernateUtil.getSessionFACTORY().openSession();

        BufferedReader reader = new BufferedReader(new FileReader(new File("./src/test/resources/EVENT_LOG_1")));
        ParserSpark parserSpark = new ParserSpark(experimentRun, session);
        parserSpark.parse(reader);

        session.close();

        //Test1 with Subtask Number 2850 and TaskType ResultTask
        Task taskResultTask = experimentRun.taskByTaskType("ResultTask");
        TaskInstance taskInstanceResultTask2850 = taskResultTask.taskInstanceBySubtaskNumber(2850);
        Date launchResultTask2850 = new Date(1414094815532L);
        Date finishedResultTask2850 = new Date(1414094815549L);
        assertEquals(taskInstanceResultTask2850.getEventByName("Launch").getValueTimestamp(), launchResultTask2850);
        assertEquals(taskInstanceResultTask2850.getEventByName("Finished").getValueTimestamp(), finishedResultTask2850);

        //Test2 with Subtask Number 2885 and TaskType ShuffleMapTask
        Task taskShuffle = experimentRun.taskByTaskType("ShuffleMapTask");
        TaskInstance taskInstanceShuffle2885 = taskShuffle.taskInstanceBySubtaskNumber(2885);
        Date launchShuffle2885 = new Date(1414094815615L);
        Date finishedShuffle2885 = new Date(1414094817066L);
        assertEquals(taskInstanceShuffle2885.getEventByName("Launch").getValueTimestamp(), launchShuffle2885);
        assertEquals(taskInstanceShuffle2885.getEventByName("Finished").getValueTimestamp(), finishedShuffle2885);
    }

    @Test
    public void testJobEntries() throws Exception{
        session = HibernateUtil.getSessionFACTORY().openSession();

        //setup Mock and Test Data
        String input = "{\"Event\":\"SparkListenerApplicationStart\",\"App Name\":\"Page Rank\",\"Timestamp\":1417077610978,\"User\":\"peel\"}";
        String input2 = "{\"Event\":\"SparkListenerTaskEnd\",\"Stage ID\":1,\"Stage Attempt ID\":0,\"Task Type\":\"ShuffleMapTask\",\"Task End Reason\":{\"Reason\":\"Success\"},\"Task Info\":{\"Task ID\":304,\"Index\":125,\"Attempt\":0,\"Launch Time\":1417077670650,\"Executor ID\":\"16\",\"Host\":\"wally007.cit.tu-berlin.de\",\"Locality\":\"PROCESS_LOCAL\",\"Speculative\":false,\"Getting Result Time\":0,\"Finish Time\":1417077755063,\"Failed\":false,\"Accumulables\":[]},\"Task Metrics\":{\"Host Name\":\"wally007.cit.tu-berlin.de\",\"Executor Deserialize Time\":43,\"Executor Run Time\":84342,\"Result Size\":2784,\"JVM GC Time\":5554,\"Result Serialization Time\":0,\"Memory Bytes Spilled\":511381957,\"Disk Bytes Spilled\":90287208,\"Shuffle Read Metrics\":{\"Shuffle Finish Time\":-1,\"Remote Blocks Fetched\":170,\"Local Blocks Fetched\":9,\"Fetch Wait Time\":553,\"Remote Bytes Read\":58917333},\"Shuffle Write Metrics\":{\"Shuffle Bytes Written\":42514740,\"Shuffle Write Time\":171796044},\"Input Metrics\":{\"Data Read Method\":\"Memory\",\"Bytes Read\":119414961},\"Updated Blocks\":[{\"Block ID\":\"rdd_6_125\",\"Status\":{\"Storage Level\":{\"Use Disk\":false,\"Use Memory\":true,\"Use Tachyon\":false,\"Deserialized\":true,\"Replication\":1},\"Memory Size\":119414961,\"Tachyon Size\":0,\"Disk Size\":0}}]}}\n";
        String input3 = "{\"Event\":\"SparkListenerTaskEnd\",\"Stage ID\":11,\"Stage Attempt ID\":0,\"Task Type\":\"ShuffleMapTask\",\"Task End Reason\":{\"Reason\":\"Success\"},\"Task Info\":{\"Task ID\":1684,\"Index\":65,\"Attempt\":0,\"Launch Time\":1417078416241,\"Executor ID\":\"10\",\"Host\":\"wally018.cit.tu-berlin.de\",\"Locality\":\"PROCESS_LOCAL\",\"Speculative\":false,\"Getting Result Time\":0,\"Finish Time\":1417078428144,\"Failed\":false,\"Accumulables\":[]},\"Task Metrics\":{\"Host Name\":\"wally018.cit.tu-berlin.de\",\"Executor Deserialize Time\":6,\"Executor Run Time\":11885,\"Result Size\":1184,\"JVM GC Time\":812,\"Result Serialization Time\":0,\"Memory Bytes Spilled\":0,\"Disk Bytes Spilled\":0,\"Shuffle Read Metrics\":{\"Shuffle Finish Time\":-1,\"Remote Blocks Fetched\":152,\"Local Blocks Fetched\":8,\"Fetch Wait Time\":556,\"Remote Bytes Read\":47588841},\"Shuffle Write Metrics\":{\"Shuffle Bytes Written\":3557232,\"Shuffle Write Time\":5266214}}}\n";
        String input4 = "{\"Event\":\"SparkListenerJobEnd\",\"Job ID\":0,\"Job Result\":{\"Result\":\"JobSucceeded\"}}\n";
        BufferedReader reader = EasyMock.createMock(BufferedReader.class);

        EasyMock.expect(reader.readLine()).andReturn(input);
        EasyMock.expect(reader.readLine()).andReturn(input2);
        EasyMock.expect(reader.readLine()).andReturn(input3);
        EasyMock.expect(reader.readLine()).andReturn(input4);
        EasyMock.expect(reader.readLine()).andReturn(null);
        EasyMock.replay(reader);

        Date submit = new Date(1417077610978L);
        Date deployed = new Date(1417077670650L);
        Date end = new Date(1417078428144L);

        Parser parser = new ParserSpark(experimentRun, session);
        parser.parse(reader);
        session.close();

        assertEquals(submit, experimentRun.getSubmitTime());
        assertEquals(deployed, experimentRun.getDeployed());
        assertEquals(end, experimentRun.getFinished());
    }

}