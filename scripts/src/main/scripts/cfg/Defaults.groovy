/* 
 * Maltcms, modular application toolkit for chromatography-mass spectrometry. 
 * Copyright (C) 2008-2013, The authors of Maltcms. All rights reserved.
 *
 * Project website: http://maltcms.sf.net
 *
 * Maltcms may be used under the terms of either the
 *
 * GNU Lesser General Public License (LGPL)
 * http://www.gnu.org/licenses/lgpl.html
 *
 * or the
 *
 * Eclipse Public License (EPL)
 * http://www.eclipse.org/org/documents/epl-v10.php
 *
 * As a user/recipient of Maltcms, you may choose which license to receive the code 
 * under. Certain files or entire directories may not be covered by this 
 * dual license, but are subject to licenses compatible to both LGPL and EPL.
 * License exceptions are explicitly declared in all relevant files or in a 
 * LICENSE file in the relevant directories.
 *
 * Maltcms is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. Please consult the relevant license documentation
 * for details.
 */
java {
    binary = "/usr/bin/java"
    //configuration option for JavaFilePipe
    virtualMemoryUnit = "m"
    virtualMemoryLowerBound = 16
    virtualMemoryUpperBound = 256
    minHeap = "-Xms${virtualMemoryLowerBound}${virtualMemoryUnit}"
    maxHeap = "-Xmx${virtualMemoryUpperBound}${virtualMemoryUnit}"
    cpusPerJob = 1
}

qsub {
    useQSub = false
    //whether to submit to an idle queue
    idleQueue = false
    //the qsub command to use
    qsub = "qsub"
    //first line of shell scripts after #!
    bashString = "/usr/bin/env bash"
    //queue to use
    queue = "main.q@@allhosts"
    //the parallel environment to allocate
    parallelEnvironment = []
    //per job resource pre-allocation
    queueResources = ["arch=lx26-amd64"]
	//environment variables to export to the drmaa job
    environmentVariables = []
    //prefix for job names
    jobNamePrefix = "maltcms"
    //maximum number of times to resubmit failing jobs
    //can be necessary on crowded grid systems due to resource exhaustion
    maxRetries = 3
}

execution {
    //limit of parallel threads to use
    maxThreads = Runtime.getRuntime().availableProcessors()-1
    //larger numbers only recommended for large cluster systems with 'useQSub=true'
    //print process output to stdout/stderr (for debugging only!)
    consumeProcessOutput = false
}

maltcms {
	threads = 4
}

/*
 * Define specific overrides of default values
 */
environments {
    local {
    }
    cluster {
        java {
            //binary = "/vol/java-7/bin/java"
            binary = "/usr/bin/java"
            //immediately reserves a fixed amount of memory to avoid
            //resource stealing by other processes on the same machine
            virtualMemoryLowerBound = "${java.virtualMemoryUpperBound}"
        }
        qsub {
            useQSub = true
			queue = "main.q@@allhosts"
			queueResources = ["arch=lx26-amd64"]
        }
        execution {
            maxThreads = 3
        }
    }
	clusterCebitec {
		java {
            binary = "/vol/java-7/bin/java"
            //immediately reserves a fixed amount of memory to avoid
            //resource stealing by other processes on the same machine
            virtualMemoryLowerBound = "${java.virtualMemoryUpperBound}"
			cpusPerJob = 1
        }
		qsub {
            useQSub = true
			idleQueue = false
			queue = "all.q@@qics"
			parallelEnvironment = ["-pe","multislot","${java.cpusPerJob}"]
			queueResources = ["arch=lx24-amd64"]
			environmentVariables = ["R_LIBS=/vol/cluster-data/hoffmann/r-modules/"]
        }
		execution {
            maxThreads = 100
        }
	}
}
