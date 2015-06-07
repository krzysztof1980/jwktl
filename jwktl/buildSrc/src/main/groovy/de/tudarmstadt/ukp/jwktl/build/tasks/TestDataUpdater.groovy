package de.tudarmstadt.ukp.jwktl.build.tasks

import de.tudarmstadt.ukp.jwktl.parser.IWiktionaryPageParser
import de.tudarmstadt.ukp.jwktl.parser.WiktionaryDumpParser
import de.tudarmstadt.ukp.jwktl.parser.de.components.DEWordLanguageHandler
import de.tudarmstadt.ukp.jwktl.parser.util.ParsingContext

/**
 * @author Krzysztof Witukiewicz
 */
class TestDataUpdater {

    private testDataDir
    private logger

    TestDataUpdater(testDataDir, logger) {
        this.testDataDir = testDataDir
        this.logger = logger
    }

    void updateTestData(xmlDump) {
        def wordsFilesMap = createWordsFilesMap()
        logger.quiet wordsFilesMap.size() + " files to update"
        def wordsTextMap = new HashMap()
        wordsFilesMap.each {
            word, _ -> wordsTextMap[word] = null
        }
        def WiktionaryDumpParser parser = new WiktionaryDumpParser(new IWiktionaryPageParser[0])
        parser.register(new WiktionaryPageParser(wordsTextMap))
        parser.parse(xmlDump)
        wordsFilesMap.each { word, file ->
            def text = wordsTextMap[word]
            file.setText(text)
            logger.quiet "Set text of length=${text.length()} in file $file.name"
        }
    }

    private Map<String, String> createWordsFilesMap() {
        def wordsFilesMap = new HashMap()
        def lemmaHandler = new DEWordLanguageHandler()
        def parsingContext = new ParsingContext(null)
        testDataDir.eachFile { file ->
            for (line in file) {
                if (lemmaHandler.canHandle(line)) {
                    lemmaHandler.fillContent(parsingContext)
                    wordsFilesMap[parsingContext.header] = file
                    logger.quiet "Added file $file.name for processing"
                    break
                }
            }
        }
        return wordsFilesMap
    }

}
