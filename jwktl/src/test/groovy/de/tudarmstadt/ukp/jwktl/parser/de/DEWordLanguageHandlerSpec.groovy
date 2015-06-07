package de.tudarmstadt.ukp.jwktl.parser.de

import de.tudarmstadt.ukp.jwktl.api.util.Language
import de.tudarmstadt.ukp.jwktl.parser.de.components.DEWordLanguageHandler
import de.tudarmstadt.ukp.jwktl.parser.util.ParsingContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Feature specification for {@link DEWordLanguageHandler}.
 *
 * @author Krzysztof Witukiewicz
 */
@Unroll
class DEWordLanguageHandlerSpec extends Specification {

    def "canHandle('#header') == #result"() {
        expect:
        new DEWordLanguageHandler().canHandle(header) == result

        where:
        header                                          || result
        "== Aberration ({{Sprache|Deutsch}}) =="        || true
        "=== {{Wortart|Substantiv|Deutsch}}, {{f}} ===" || false
        "{{Deutsch Substantiv f schwach"                || false
    }

    def "lemma='#lemma' and language='#language' for '#header'"() {
        given:
        DEWordLanguageHandler handler = new DEWordLanguageHandler()
        ParsingContext parsingContext = Mock()

        when:
        handler.canHandle(header)
        handler.fillContent(parsingContext)

        then:
        1 * parsingContext.setHeader(lemma)
        1 * parsingContext.setLanguage(language)

        where:
        header                                      || lemma            | language
        "== Aberration ({{Sprache|Deutsch}}) =="    || "Aberration"     | Language.GERMAN
        "== robber baron ({{Sprache|Englisch}}) ==" || "robber baron"   | Language.ENGLISH
    }
}