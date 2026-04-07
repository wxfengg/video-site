import { createRouter, createWebHistory } from "vue-router"
import type { RouteRecordRaw } from "vue-router"
import { fetchAdminSession } from "../utils/auth"
import { fetchUserSession } from "../apis/user"

const routes: RouteRecordRaw[] = [
  {
    path: "/",
    component: () => import("../layouts/PublicLayout.vue"),
    children: [
      {
        path: "",
        name: "Home",
        component: () => import("../views/home/HomeRecommendView.vue"),
      },
      {
        path: "videos/:id",
        name: "VideoPlayer",
        component: () => import("../views/video/VideoPlayerView.vue"),
      },
      {
        path: "user/login",
        name: "UserLogin",
        component: () => import("../views/user/UserLoginView.vue"),
        meta: {
          guestUserOnly: true,
        },
      },
      {
        path: "user/register",
        name: "UserRegister",
        component: () => import("../views/user/UserRegisterView.vue"),
        meta: {
          guestUserOnly: true,
        },
      },
      {
        path: "me/favorites",
        name: "UserFavorites",
        component: () => import("../views/user/UserFavoritesView.vue"),
        meta: {
          requiresUser: true,
        },
      },
      {
        path: "me/history",
        name: "UserHistory",
        component: () => import("../views/user/UserHistoryView.vue"),
        meta: {
          requiresUser: true,
        },
      },
    ],
  },
  {
    path: "/admin/login",
    name: "AdminLogin",
    component: () => import("../views/admin/AdminLoginView.vue"),
    meta: {
      guestOnly: true,
    },
  },
  {
    path: "/admin",
    component: () => import("../layouts/AdminLayout.vue"),
    meta: {
      requiresAdmin: true,
    },
    children: [
      {
        path: "",
        name: "AdminDashboard",
        component: () => import("../views/admin/DashboardView.vue"),
      },
      {
        path: "upload",
        name: "AdminVideoUpload",
        component: () => import("../views/admin/VideoUploadView.vue"),
      },
      {
        path: "videos",
        name: "AdminVideoManage",
        component: () => import("../views/admin/VideoManageView.vue"),
      },
      {
        path: "users",
        name: "AdminUserManage",
        component: () => import("../views/admin/UserManageView.vue"),
      },
      {
        path: "ab-experiments",
        name: "AdminAbExperiments",
        component: () => import("../views/admin/AbExperimentView.vue"),
      },
      {
        path: "ab-reports",
        name: "AdminAbReports",
        component: () => import("../views/admin/AbReportView.vue"),
      },
    ],
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  if (to.meta.requiresAdmin) {
    const session = await fetchAdminSession()
    if (!session.loggedIn) {
      return {
        path: "/admin/login",
        query: {
          redirect: to.fullPath,
        },
      }
    }
  }

  if (to.meta.guestOnly) {
    const session = await fetchAdminSession()
    if (session.loggedIn) {
      return {
        path: "/admin",
      }
    }
  }

  if (to.meta.requiresUser) {
    const session = await fetchUserSession()
    if (!session.loggedIn) {
      return {
        path: "/user/login",
        query: {
          redirect: to.fullPath,
        },
      }
    }
  }

  if (to.meta.guestUserOnly) {
    const session = await fetchUserSession()
    if (session.loggedIn) {
      return {
        path: "/",
      }
    }
  }

  return true
})
