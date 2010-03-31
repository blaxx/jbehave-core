package org.jbehave.core;

import static org.jbehave.Ensure.ensureThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.hamcrest.Matchers;
import org.jbehave.core.steps.CandidateSteps;
import org.junit.Test;

public class RunnableStoryBehaviour {

    @Test
    public void shouldRunUsingTheStoryRunner() throws Throwable {
        // Given
        StoryRunner runner = mock(StoryRunner.class);
        StoryConfiguration configuration = mock(StoryConfiguration.class);
        CandidateSteps steps = mock(CandidateSteps.class);
        Class<MyStory> storyClass = MyStory.class;

        // When
        RunnableStory story = new MyStory(runner, configuration, steps);
        story.runStory();

        // Then
        verify(runner).run(storyClass, configuration, steps);
    }
    
    @Test
    public void shouldAllowOverrideOfDefaultConfiguration() throws Throwable {
        // Given
        StoryRunner runner = mock(StoryRunner.class);
        StoryConfiguration configuration = mock(StoryConfiguration.class);
        CandidateSteps steps = mock(CandidateSteps.class);
        Class<MyStory> storyClass = MyStory.class;

        // When
        RunnableStory story = new MyStory(runner, steps);
        ensureThat(story.getConfiguration(),  Matchers.not(Matchers.sameInstance(configuration)));
        story.useConfiguration(configuration);
        story.runStory();

        // Then
        ensureThat(!(story.getConfiguration() instanceof PropertyBasedStoryConfiguration));
        verify(runner).run(storyClass, configuration, steps);
    }

    
    @Test
    public void shouldAllowAdditionOfSteps() throws Throwable {
        // Given
        StoryRunner runner = mock(StoryRunner.class);
        StoryConfiguration configuration = mock(StoryConfiguration.class);
        CandidateSteps steps = mock(CandidateSteps.class);
        Class<MyStory> storyClass = MyStory.class;

        // When
        RunnableStory story = new MyStory(runner, configuration);
        story.addSteps(steps);
        story.runStory();

        // Then
        verify(runner).run(storyClass, configuration, steps);
    }

    private class MyStory extends JUnitStory {

        public MyStory(StoryRunner runner, CandidateSteps... steps) {
            super(runner);
            addSteps(steps);
        }

        public MyStory(StoryRunner runner, StoryConfiguration configuration, CandidateSteps... steps) {
            super(runner);
            useConfiguration(configuration);
            addSteps(steps);
        }

    }

}