## TODOs

* Rework the hmrc-frontend-scaffold.g8 template to use init_service package structure or create a new template
* Special care needs to be taken to preserve the changes to the frontend-scaffold:
  * Changed `RichJsObject` to use `deepMerge`, preserving nested answers
  * Changed `Page` to have `AnswerType`
    * Not sure whether this is the final change as scalac doesn't actually seem to check it
  * Changed `QuestionPage` to have `AnswerType` and `submitRoute`
    * May wish to add `loadRoute` too for convenience?
  * Added passthrough to `journey.Routes` in `prod.routes`
* Rework the generated code to make use of the package structure
* Implement switch-case else / default functionality?
* Consider whether to use the `journeys.<journey name>` to build a sub-package structure (i.e. fractal application structure)
  * This would lessen the chance of reverse routes collisions
  * However it isn't common practice in the rest of HMRC Digital
  * It could be done just for controllers rather than the entire application
* Use `String#indent` rather than padding vars once we're on the right JDK everywhere
* More testing around nested models and form binding
* More testing around reuse of pages in different "forks" of a journey