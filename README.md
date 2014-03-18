## Evaluation scripts for BiPACE-2D

Author: nils.hoffmann@cebitec.uni-bielefeld.de   
Version: 1.9   
Last updated: Mar. 18th, 2014   

Changes:   

* 1.9:
    + Modularized build, pushed to github
* 1.8:
    + Updated Maltcms Version to 1.3
    + Updated scripts for plot generation
* 1.7:
    + Integrated Guineu
    + Updated evaluation to include pairwise alignment performance
    + Updated evaluation graphics scripts
* 1.6:
    + Updated maltcms configuration and version
* 1.5:
    + Cleaned up directories, relocated maltcms
* 1.4:
    + Updated references
* 1.3:
    + Fixed manual reference problems for chlamy_Dataset_I
    + Updated references
* 1.2:
    + Removed old scripts.
    + Relocated output of evaluations to directory "results"
* 1.1:
    + Fixed R package installation function typo.
* 1.0:
    + Initial version with evaluation scripts and data.

***

### Citation

If you use any of the material provided in this distribution, we kindly ask
you to cite the following publications:

 Hoffmann et al., "BiPACE2D - Graph-based multiple alignment for comprehensive
 two-dimensional gas chromatography-mass spectrometry", Bioinformatics, 2013;
 doi: 10.1093/bioinformatics/btt738

 Hoffmann et al., "Combining peak- and chromatogram-based retention time
 alignment algorithms for multiple chromatography-mass spectrometry
 datasets", BMC Bioinformatics, 2012, 13:214, doi:10.1186/1471-2105-13-214

More specific information on the supplied datasets and their original
publications may be found in Section 4 of this document.

### 1. License:

The source code of this distribution is licensed under the GNU Lesser
General Public License version 3 or under the terms of the Eclipse Public 
License version 1. Details may be found in the LICENSE.LGPL and LICENSE.EPL
files. Guineu and the customizations are licensed under the GNU GPL license.
mSPA and SWPA have been included for scientific evaluation and are not covered
by any of the above licenses.

Please see section 4 for details on the datasets contained in this
distribution.

### 2. Requirements

* a Unix-compatible operating system (Linux or MacOS X).
* a recent JAVA SDK, 7, a.k.a 1.7.
* a recent version of gradle (www.gradle.org), version 1.6.+, please
 follow their installation instructions for your system.
* a recent installation of GNU-R (www.r-project.org) > 3.0 with 'optparse'
 and 'ggplot2', 'plyr', and 'xtable'. To install from R's command line:   
 `> install.packages(c("optparse","ggplot2","plyr","xtable"))`
* an online connection, at least for the bootstrapping phase (see section 3)

***

If you experience problems building and/or running the project, please
contact the author for assistance. (see head of this document)

### 3. Running the evaluation:

Please note that the evaluation will place a HUGE workload on your
computer for a long time. Total runtime depends largely on the
performance of your system.

The default settings for the evaluations concerning parallel execution
have been adapted to allow immediate execution on stand-alone computer
hardware for the 'mSPA_Dataset_I_short' configuration.

At your command prompt, enter

    >bin/runEvaluation.sh -l

to list the evaluation configurations that are available.

Select one of those evaluations (the _short one is for demonstration only):

    >bin/runEvaluation.sh -n mSPA_Dataset_I_short

The evaluation will begin by bootstrapping (downloading) the required
external libraries before compiling the evaluation code. It will then run
the evaluation and print progress on what it is currently running. The evaluation
will by default run using the local profile (-p local, below etc/ directory).
If you plan to run the evaluation on a grid system, please customize 
one of the cluster or clusterCebitec profiles for your own needs and run the 
evaluation like:

    >bin/runEvaluation.sh -n mSPA_Dataset_I -p cluster

You may alternatively also submit the evaluation script itself to run on 
the cluster using the followin command:

    >bin/runGridEvaluation.sh -n mSPA_Dataset_I -p cluster

Finally, the evaluation will create output in the

    results/mSPA_Dataset_I_short/evaluation

directory.

The output consists of excel compatible tables holding for each evaluated
instance its parametrization, alignment performance metrics, runtime and
memory consumption. Additionally, the directory contains various plots
of those metrics.

You can find a list of available configurations in the 'instances.txt' file.
The configurations themselves are located in the

    scripts/src/main/scripts/cfg/

folder. Within that folder, the file 'Defaults.groovy' contains the default
values for parallel execution (local host and grid engine). This file
also allows to set an explicit path for the java binary used during evaluation.

The peak lists and derived reference multiple alignments are located in the
following folders:

for MSPA EVALUATION:
`mSPA/`

for SWPA EVALUATION:
`SWPA/`

for CHLAMY EVALUATION:
`chlamy/`

All folders have the same substructure ('data','groundTruth') and contain
further 'README' files with additional information.

#### 3.1 Customization:

To change the number of parallel processes, change the

'maxThreads' property (under 'execution') in

`scripts/src/main/scripts/cfg/Defaults.groovy`

to the number of cpus available on your system.

Please also set the value

'useQSub'

under 'environments cluster qsub' to 'false' to turn off the use of grid submission.
Finally, the number of cpus to use for each Maltcms instance

    cross.Factory.maxthreads = 4

can be set in
`cfg/cemappDtw.properties` and `cfg/cemappDtwRt.properties` and should be set to one.
This value should be matched by

    cpusPerJob = 4

under 'java' `in scripts/src/main/scripts/cfg/Defaults.groovy`.

Instance-specific configuration may be found in the respective `*.groovy` files
below `scripts/src/main/scripts/cfg/`. For example the configuration for mSPA_Dataset_I
is to be found in `scripts/src/main/scripts/cfg/mSPA_Dataset_I.groovy`

***

In order to save some memory, you can set

    cross.datastructures.fragments.FileFragment.useCachedList = true

in `cfg/evaluationDefaults.properties`. This will limit the number of mass spectra
kept in memory.

***

#### 3.2 Known issues

Due to distributed file system synchronization issues during a grid-based evaluation,
the evaluation may fail in rare cases. We have implemented waiting times between completion 
of the drmaa API calls and the post processing of file results. Should you
encounter this issue, please contact the author.

### 4. Notes and References

#### Evaluation scripts, source-code, and assets

 Hoffmann et al., "BiPACE2D - Graph-based multiple alignment for comprehensive
 two-dimensional gas chromatography-mass spectrometry", Bioinformatics, 2013;
 doi: 10.1093/bioinformatics/btt738

 Hoffmann et al., "Combining peak- and chromatogram-based retention time alignment algorithms
 for multiple chromatography-mass spectrometry datasets", BMC Bioinformatics, 2012,
 13:214, doi:10.1186/1471-2105-13-214

 The source code of this evaluation, except for the mSPA and SWPA implementations,
 is licensed under the Lesser GNU General Public License version 3 or the Eclipse Public License,
 version 1. The license text is contained in the files LICENSE.LGPL and LICENSE.EPL in the same
 directory as this README.

 Maltcms, the framework behind BiPACE, BiPACE2D is available at
 http://maltcms.sf.net

#### mSPA samples (directory 'mSPA')

 The original scripts were altered to allow more diagnostic and graphical output.
 Use the --plot=TRUE option for mSPA-evaluation.R to see the plots.
 We added the MGMA method to select peaks occurring multiple times with identical name
 annotation based on pre-defined standard deviations in retention time (rt) in the first (sd1Thres)
 and second (sd2Thres) rt dimensions across all chromatograms. This builds the basis
 for reference selection and definition of the multiple-alignment ground truth that
 is later used during alignment evaluation and avoids some of the shortcomings of
 the original approach.

 Please consult the mSPA/README file for further information.

 Within the mSPA/mspa directory, run ./mspa-evaluation.R to see a list of options.
 The original sourcecode has been included for completeness and documentation
 purposes.

 The mSPA samples were originally published along with:

 Kim et al., "An Optimal Peak Alignment For Comprehensive Two-Dimensional Gas
 Chromatography Mass Spectrometry Using Mixture Similarity Measure", Bioinformatics, 2011,
 27:12, doi:10.1093/bioinformatics/btr188

#### SWPA samples (directory 'SWPA')

 We applied the same adaptations as for the mSPA source code. This was possible,
 since both codebases share a large amount of common functionality. In fact, SWPA
 is not executable on its own without parts of the mSPA.R code.

 Please consult the SWPA/README file for further information.

 Within the SWPA/swpa directory, run ./swpa-evaluation.R to see a list of options.
 The original sourcecode has been included for completeness and documentation
 purposes.

 The SWPA samples were originally published along with:

 Kim et al., "Smith-Waterman peak alignment for comprehensive two-dimensional
 gas chromatography-mass spectrometry", BMC Bioinformatics, 2011, 12:1,
 doi:10.1186/1471-2105-12-235

#### Chlamydomonas reinhardtii samples (directory 'chlamy')

 The original analysis was published in:

 Doebbe et al., "The Interplay of Proton, Electron, and Metabolite Supply for
 Photosynthetic {{H\_2}} Production in Chlamydomonas reinhardtii", Journal of
 Biological Chemistry, 2010, 285:39, doi:10.1074/jbc.M110.122812

 The study raw data, as well as ChromaTOF peak lists and manual reference
 alignment are available from the Metabolights database:

 http://www.ebi.ac.uk/metabolights/MTBLS37

#### Guineu (directory 'guineu')
 
 We checked out the source code of Guineu, revision 786 from
 svn checkout http://guineu.googlecode.com/svn/trunk/ guineu-read-only
 
 We modified Guineu to be able to run it without a graphical user interface 
 along the other methods within our evaluation framework. 
 Additionally, we modified Guineu's parser to include support for the 
 two retention time fields variant of ChromaTOF peak list data.
 Then, we extended the GCGC Datataset to include peak row index information
 for later reference during export of the multiple alignment. We finally 
 added a module to export a multiple alignment of the peak lists.

 The modified source code is contained in the directory 'guineu/src' and 
 requires gradle to build.

 Guineu was originally published in:

 Castillo et al., "Data Analysis Tool for Comprehensive Two-Dimensional Gas
 Chromatography/Time-of-Flight Mass Spectrometry", Analytical Chemistry,
 2011, 83:8, doi:10.1021/ac103308x

