# `@atomist/update-npm-dependencies`

<!---atomist-skill-readme:start--->

Update NPM dependencies tracks all of the NPM libraries found in project's package.json files.  The skill can be configured
to automatically create Pull Requests when a library version is discovered to be out of sync with a target policy.

## Configuration

### Name

Give your configuration of this skill a distinctive name so that you you will recognize it when you have more than one enabled. 
Something helpful like "latest releases from NpmJs.org" would be appropriate for a configuration that helps 
track bug fixes in any open source libraries used by your projects.

### policy

Each configuration of this skill allows you to choose how you track target versions of libraries.

* `manual` - this policy is appropriate when you'll manually choose a target version to be used by your projects.  As part of configuring 
             the manual policy, you'll enter an `application/json` formatted map of dependencies.  This is how dependencies are written
             into a package.json file so it can be copied out of an existing package.json file.
           
Here is an example of defining a policy for which version of `express` should be used by any package.json file found in your projects.

```
{"express": "4.17.1"}
```

* `latest semver available` - this policy should be used for libraries that should track latest releases on npmjs.org.  When configuring this policy,
                              you do not need to reference any versions.  Versions will automatically flow into your projects from npmjs.org.
                              
When configuring this policy, you must choose which npm libraries should track the latest available semver.  
If you want new releases of `express` to flow into your projects, then you'd add the library name to an application/json encoded String Array like this: 

```
["express"]
``` 

* `latest semver used` - this policy will detect the latest version used within your set of Repositories, and then pull along
                         other projects that might be using an older version.  This permits a skill user to instruct some
                         projects to "follow" other projects, that have moved on to new versions.
                         
The configuration is similar to the one for `latest semver available`.  The policy should reference the name of each library
that should adhere to this policy using an `application/json` encoded String array.   
                         
```
["@atomist/common", "express"]
```                         

### Which repositories

By default, this skill will be enabled for all repositories in all organizations you have connected.
To restrict the organizations or specific repositories on which the skill will run, you can explicitly
choose organization(s) and repositories.

## Integrations

**GitHub**

The Atomist GitHub integration must be configured to used this skill. At least one repository must be selected.

**Slack**

If the Atomist Slack integration is configured, this skill will send a notification message to the configured Slack channel when a pull request is created. 

<!---atomist-skill-readme:end--->

---

Created by [Atomist][atomist].
Need Help?  [Join our Slack workspace][slack].

[atomist]: https://atomist.com/ (Atomist - How Teams Deliver Software)
[slack]: https://join.atomist.com/ (Atomist Community Slack) 
