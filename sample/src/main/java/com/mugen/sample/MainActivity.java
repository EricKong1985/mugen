package com.mugen.sample;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mugen.Mugen;
import com.mugen.MugenCallbacks;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

        private String query = "android";
        private String language = "java";
        private String queryString = "%s+language:%s";
        int currentPage = 1;
        boolean isLoading = false;

        SwipeRefreshLayout mSwipeRefreshLayout;
        RecyclerView mRecyclerView;
        RepoAdapter mRepoAdapter;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
            mSwipeRefreshLayout.setOnRefreshListener(this);
            mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
            LinearLayoutManager manager = new LinearLayoutManager(getActivity(),
                    LinearLayoutManager.VERTICAL,
                    false);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(manager);
            mRecyclerView.setAdapter(mRepoAdapter = new RepoAdapter(null));
            loadData(query, language, currentPage, false);
            return rootView;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            Mugen.with(mRecyclerView, new MugenCallbacks() {
                @Override
                public void onLoadMore() {
                    loadData(query, language, currentPage + 1, false);
                }

                @Override
                public boolean isLoading() {
                    return isLoading;
                }

                @Override
                public boolean hasLoadedAllItems() {
                    return !isLoading;
                }
            });

        }

        private void loadData(final String query, final String language, final int page, final boolean isRefreshed) {
            new AsyncTask<Integer, Void, List<GitHubClient.Repo>>() {

                @Override
                protected List<GitHubClient.Repo> doInBackground(Integer... params) {
                    String q = String.format(Locale.ENGLISH,
                            queryString,
                            query,
                            language);
                    isLoading = true;
                    return GitHubClient.getClient()
                            .searchRepos(q,
                                    GitHubClient.DEFAULT_SORT,
                                    GitHubClient.DEFAULT_ORDER,
                                    params[0]).repos;
                }

                @Override
                protected void onPostExecute(List<GitHubClient.Repo> repos) {
                    isLoading = false;
                    mRepoAdapter.onNext(repos, isRefreshed);
                    currentPage = page;
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }.execute(page);
        }

        @Override
        public void onRefresh() {
            loadData(query, language, currentPage, true);
        }
    }

    private static class RepoAdapter extends RecyclerView.Adapter<RepoHolder> {

        List<GitHubClient.Repo> repoList;

        public RepoAdapter(List<GitHubClient.Repo> repos) {
            if (repos == null) {
                repos = new ArrayList<GitHubClient.Repo>();
            }
            this.repoList = repos;
        }

        @Override
        public RepoHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_repo, viewGroup, false);
            return new RepoHolder(view);
        }

        @Override
        public void onBindViewHolder(RepoHolder repoHolder, int i) {
            GitHubClient.Repo repo = repoList.get(i);
            repoHolder.textRepo.setText(repo.name);
            repoHolder.textUser.setText(repo.owner.login);
            repoHolder.textStars.setText(repo.starsGazers + "");
            repoHolder.textForks.setText(repo.forks + "");

            String imgUrl = repo.owner.avatarUrl;
            if (imgUrl != null && !imgUrl.equals("")) {
                Picasso.with(repoHolder.imageAvatar.getContext())
                        .load(imgUrl)
                        .resize(200, 200)
                        .error(R.drawable.ic_github_placeholder)
                        .placeholder(R.drawable.ic_github_placeholder)
                        .centerCrop()
                        .into(repoHolder.imageAvatar);
            }
        }

        @Override
        public int getItemCount() {
            return repoList.size();
        }

        public void onNext(List<GitHubClient.Repo> repos, boolean top) {
            if (repos == null) {
                return;
            }
            if (repoList == null) {
                repoList = new ArrayList<GitHubClient.Repo>();
            }
            if (top) {
                repoList.addAll(0, repos);
            } else {
                repoList.addAll(repos);
            }

            notifyDataSetChanged();
        }
    }

    private static class RepoHolder extends RecyclerView.ViewHolder {

        ImageView imageAvatar;
        TextView textRepo;
        TextView textUser;
        TextView textStars;
        TextView textForks;

        public RepoHolder(View itemView) {
            super(itemView);
            imageAvatar = (ImageView) itemView.findViewById(R.id.imageView_avatar);
            textRepo = (TextView) itemView.findViewById(R.id.textView_repo_name);
            textUser = (TextView) itemView.findViewById(R.id.textView_user_name);
            textStars = (TextView) itemView.findViewById(R.id.textView_stars);
            textForks = (TextView) itemView.findViewById(R.id.textView_forks);
            ((ImageView) itemView.findViewById(R.id.imageView_triangle)).
                    setColorFilter(itemView
                            .getContext()
                            .getResources()
                            .getColor(R.color.blue_light));


        }
    }
}
