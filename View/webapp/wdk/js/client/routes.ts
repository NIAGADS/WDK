import { RouteSpec } from './CommonTypes';

import IndexController from './controllers/IndexController';
import RecordController from './controllers/RecordController';
import NotFoundController from './controllers/NotFoundController';
import AnswerController from './controllers/AnswerController';
import QuestionListController from './controllers/QuestionListController';
import DownloadFormController from './controllers/DownloadFormController';
import UserRegistrationController from './controllers/UserRegistrationController';
import UserProfileController from './controllers/UserProfileController';
import UserPasswordChangeController from './controllers/UserPasswordChangeController';
import UserPasswordResetController from './controllers/UserPasswordResetController';
import UserMessageController from './controllers/UserMessageController';
import SiteMapController from './controllers/SiteMapController';
import UserDatasetListController from './controllers/UserDatasetListController';
import UserDatasetItemController from './controllers/UserDatasetItemController';
import FavoritesController from './controllers/FavoritesController';
import QuestionController from './controllers/QuestionController';

export default <RouteSpec[]> [
  { path: '/', component: IndexController },
  { path: '/search/:recordClass/:question/result', component: AnswerController },
  { path: '/search/:recordClass/:question', component: QuestionController },
  { path: '/record/:recordClass/download/:primaryKey+', component: DownloadFormController },
  { path: '/record/:recordClass/:primaryKey+', component: RecordController },
  { path: '/step/:stepId/download', component: DownloadFormController },
  { path: '/user/registration', component: UserRegistrationController },
  { path: '/user/profile', component: UserProfileController },
  { path: '/user/profile/password', component: UserPasswordChangeController },
  { path: '/user/forgot-password', component: UserPasswordResetController },
  { path: '/user/message/:messageKey', component: UserMessageController },
  { path: '/workspace/datasets', component: UserDatasetListController },
  { path: '/workspace/datasets/:id', component: UserDatasetItemController },
  { path: '/favorites', component: FavoritesController },
  { path: '/data-finder', component: SiteMapController },
  { path: '/question-list', component: QuestionListController },
  { path: '*', component: NotFoundController },
];