package de.tudarmstadt.ukp.jwktl.build.tasks

import de.tudarmstadt.ukp.jwktl.parser.IWiktionaryPageParser
import de.tudarmstadt.ukp.jwktl.parser.util.IDumpInfo

/**
 * @author Krzysztof Witukiewicz
 */
class WiktionaryPageParser implements IWiktionaryPageParser {

    private Map<String, String> words
    private String currWord

    WiktionaryPageParser(Map<String, String> words) {
        this.words = words
    }

    @Override
    void onParserStart(IDumpInfo dumpInfo) {

    }

    @Override
    void onSiteInfoComplete(IDumpInfo dumpInfo) {

    }

    @Override
    void onParserEnd(IDumpInfo dumpInfo) {

    }

    @Override
    void onClose(IDumpInfo dumpInfo) {

    }

    @Override
    void onPageStart() {

    }

    @Override
    void onPageEnd() {

    }

    @Override
    void setAuthor(String author) {

    }

    @Override
    void setRevision(long revisionId) {

    }

    @Override
    void setTimestamp(Date timestamp) {

    }

    @Override
    void setPageId(long pageId) {

    }

    @Override
    void setTitle(String title, String namespace) {
        currWord = namespace == null && words.containsKey(title) ? title : null
    }

    @Override
    void setText(String text) {
        if (currWord != null) {
            words[currWord] = text
        }
    }
}
