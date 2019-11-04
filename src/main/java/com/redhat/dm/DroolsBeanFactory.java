package com.redhat.dm;

import java.util.Collection;
import java.util.Collections;

import org.appformer.maven.support.AFReleaseId;
import org.appformer.maven.support.DependencyFilter;
import org.appformer.maven.support.PomModel;
import org.drools.compiler.kie.builder.impl.KieBuilderImpl;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;

public class DroolsBeanFactory {
    private KieServices kieServices=KieServices.Factory.get();

    private void getKieRepository() {
        final KieRepository kieRepository = kieServices.getRepository();
        kieRepository.addKieModule(new KieModule() {
                        public ReleaseId getReleaseId() {
                return kieRepository.getDefaultReleaseId();
            }
        });
    }

    public KieSession getKieSession(){
        getKieRepository();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        kieFileSystem.write(ResourceFactory.newClassPathResource("rules/DetermineConsensus.drl"));

        
        
        KieBuilder kb = kieServices.newKieBuilder(kieFileSystem);
        ((KieBuilderImpl) kb).setPomModel(new PomModel() {

			@Override
			public Collection<AFReleaseId> getDependencies() {
				return Collections.emptyList();
			}

			@Override
			public Collection<AFReleaseId> getDependencies(DependencyFilter filter) {
				return Collections.emptyList();
			}

			@Override
			public AFReleaseId getParentReleaseId() {
				return kieServices.getRepository().getDefaultReleaseId();
			}

			@Override
			public AFReleaseId getReleaseId() {
				// TODO Auto-generated method stub
				return kieServices.getRepository().getDefaultReleaseId();
			}});
        kb.buildAll();
        KieModule kieModule = kb.getKieModule();

        KieContainer kContainer = kieServices.newKieContainer(kieModule.getReleaseId());

        return kContainer.newKieSession();

    }
}