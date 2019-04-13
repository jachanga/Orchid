package com.eden.orchid.wiki

import com.eden.common.util.EdenUtils
import com.eden.orchid.api.OrchidContext
import com.eden.orchid.api.generators.FileCollection
import com.eden.orchid.api.generators.OrchidCollection
import com.eden.orchid.api.generators.OrchidGenerator
import com.eden.orchid.api.options.annotations.Description
import com.eden.orchid.api.options.annotations.ImpliedKey
import com.eden.orchid.api.options.annotations.Option
import com.eden.orchid.api.resources.resource.StringResource
import com.eden.orchid.api.theme.pages.OrchidPage
import com.eden.orchid.wiki.model.WikiModel
import com.eden.orchid.wiki.model.WikiSection
import com.eden.orchid.wiki.pages.WikiBookPage
import com.eden.orchid.wiki.pages.WikiSectionsPage
import java.util.stream.Stream
import javax.inject.Inject

@Description("Create a structured and navigable knowledge-base for your project.", name = "Wiki")
class WikiGenerator
@Inject
constructor(
    context: OrchidContext,
    private val wikiModel: WikiModel
) : OrchidGenerator(context, GENERATOR_KEY, OrchidGenerator.PRIORITY_EARLY) {

    companion object {
        const val GENERATOR_KEY = "wiki"
    }

    @Option
    @ImpliedKey("key")
    @Description("The sections within the baseDir to make wikis out of.")
    lateinit var sections: MutableList<WikiSection>

    @Option
    @Description("The configuration for the default wiki, when no other categories are set up.")
    lateinit var defaultConfig: WikiSection

    override fun startIndexing(): List<OrchidPage> {
        if (EdenUtils.isEmpty(sections)) {
            sections.add(defaultConfig)
        }

        val loadedSections = ArrayList<WikiSection>()
        this.sections.forEach { section ->
            val loadedSection = section.adapter.loadWikiPages(section)
            if (loadedSection != null) {
                loadedSections.add(loadedSection)
            }
        }

        wikiModel.initialize(loadedSections)

        if (loadedSections.size > 1) {
            wikiModel.sectionsPage = getSectionsIndex()
        }

        return wikiModel.allPages
    }

    override fun startGeneration(pages: Stream<out OrchidPage>) {
        pages.forEach {
            if (it is WikiBookPage) {
                context.renderBinary(it)
            } else {
                context.renderTemplate(it)
            }
        }
    }

    private fun getSectionsIndex(): WikiSectionsPage {
        val resource = StringResource(context, "wiki.md", "")

        val sectionsPage = WikiSectionsPage(wikiModel, resource, "Wiki")

        for (summaryPage in wikiModel.sections.values) {
            summaryPage.summaryPage.sectionsPage = sectionsPage
            summaryPage.summaryPage.parent = sectionsPage
        }

        return sectionsPage
    }

    override fun getCollections(): List<OrchidCollection<*>> {
        val collectionsList = java.util.ArrayList<OrchidCollection<*>>()

        wikiModel.sections.forEach {
            var sectionPages = ArrayList<OrchidPage>()

            sectionPages.add(it.value.summaryPage)
            sectionPages.addAll(it.value.wikiPages)

            val collection = FileCollection(this, it.key, sectionPages)
            collectionsList.add(collection)
        }

        return collectionsList
    }

}

