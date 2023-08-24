package no.nav.foreldrepenger.feil;

import org.jboss.jandex.*;
import org.jboss.jandex.AnnotationTarget.Kind;

import java.io.IOException;
import java.util.*;

/** Henter persistert index (hvis generert) eller genererer index for angitt location (typisk matcher en jar/war fil). */
class IndexFeil {
    private String jandexIndexFileName;

    IndexFeil() {
        this("jandex.idx");
    }

    public IndexFeil(String jandexIndexFileName) {
        this.jandexIndexFileName = jandexIndexFileName;
    }

    public IndexView getIndex() {
        return getPersistedJandexIndex();
    }

    // fra pre-generert index, slipper runtime scanning for raskere startup
    private IndexView getPersistedJandexIndex() {
        return getJandexIndex();
    }

    private IndexView getJandexIndex() {
        var classLoaders = Arrays.asList(getClass().getClassLoader(), Thread.currentThread().getContextClassLoader());

        List<IndexView> ivs = new ArrayList<>();
        classLoaders
            .stream()
            .flatMap(cl -> {
                try {
                    return Collections.list(cl.getResources("META-INF/" + jandexIndexFileName)).stream();
                } catch (IOException e2) {
                    throw new IllegalArgumentException("Kan ikke lese jandex index fil", e2);
                }
            })
            .forEach(url -> {
                try (var is = url.openStream()) {
                    var ir = new IndexReader(is);
                    ivs.add(ir.read());
                } catch (IOException e) {
                    throw new IllegalStateException("Kunne ikke lese:" + url.toExternalForm(), e);
                }
            });

        return CompositeIndex.create(ivs);
    }

    public List<AnnotationInstance> getAnnotationInstances(Class<?>... feilAnnotation) {
        Set<AnnotationInstance> annotations = new LinkedHashSet<>(1000);

        Arrays.asList(feilAnnotation).forEach(ft -> {
            var search = DotName.createSimple(ft.getName());
            annotations.addAll(getIndex().getAnnotations(search));
        });

        List<AnnotationInstance> types = new ArrayList<>();
        for (var ann : annotations) {
            if (ann.target().kind() == Kind.METHOD) {
                types.add(ann);
            }
        }

        return types;
    }
}
