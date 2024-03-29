package com.github.checkit.service;

import com.github.checkit.config.properties.GitHubConfigProperties;
import com.github.checkit.model.PublicationContext;
import java.io.IOException;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReviewEvent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GitHubService {

    private final Logger logger = LoggerFactory.getLogger(GitHubService.class);

    private final GitHubConfigProperties gitHubConfigProperties;
    private final UserService userService;

    public GitHubService(GitHubConfigProperties gitHubConfigProperties, UserService userService) {
        this.gitHubConfigProperties = gitHubConfigProperties;
        this.userService = userService;
    }

    /**
     * Approve Pull Request of specified publication context in SSP GitHub.
     *
     * @param publicationContext publication context that was approved
     */
    public void approvePullRequest(PublicationContext publicationContext) {
        if (!gitHubConfigProperties.getPublishToSSP()) {
            return;
        }
        String pullRequestUrl = publicationContext.getCorrespondingPullRequest();
        try {
            GHPullRequest pullRequest = getPullRequest(pullRequestUrl);
            pullRequest.createReview().event(GHPullRequestReviewEvent.APPROVE)
                .body(String.format("Gestor %s approved these changes in publication context \"%s\".",
                    userService.getCurrent().toSimpleString(), publicationContext.getUri())).create();
            logger.info("Pull Request \"{}\" for publication context \"{}\" was approved.",
                pullRequest.getTitle(), publicationContext.getUri());
            if (!pullRequest.isMerged()) {
                pullRequest.merge(String.format("Merge %s to %s.", pullRequest.getBase().getLabel(),
                    pullRequest.getHead().getLabel()));
            }
            logger.info("Pull Request \"{}\" for publication context \"{}\" was merged.",
                pullRequest.getTitle(), publicationContext.getUri());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Close Pull Request of specified publication context in SSP GitHub repository.
     *
     * @param publicationContext publication context that was rejected
     */
    public void closePullRequest(PublicationContext publicationContext) {
        if (!gitHubConfigProperties.getPublishToSSP()) {
            return;
        }
        String pullRequestUrl = publicationContext.getCorrespondingPullRequest();
        try {
            GHPullRequest pullRequest = getPullRequest(pullRequestUrl);
            pullRequest.comment(String.format("Gestor %s rejected these changes in publication context \"%s\".",
                userService.getCurrent().toSimpleString(), publicationContext.getUri()));
            pullRequest.close();
            logger.info("Pull Request \"{}\" for publication context \"{}\" was closed.",
                pullRequest.getTitle(), publicationContext.getUri());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private GHPullRequest getPullRequest(String pullRequestUrl) throws IOException {
        GitHub gitHub = new GitHubBuilder().withOAuthToken(gitHubConfigProperties.getToken()).build();
        GHOrganization organization = gitHub.getOrganization(gitHubConfigProperties.getOrganization());
        GHRepository repository = organization.getRepository(gitHubConfigProperties.getRepository());
        return repository.getPullRequest(getPullRequestId(pullRequestUrl));
    }

    private int getPullRequestId(String pullRequestUrl) {
        return Integer.parseInt(pullRequestUrl.substring(pullRequestUrl.lastIndexOf("/") + 1));
    }
}
